/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.properties;

import java.lang.reflect.Constructor;

import org.springframework.beans.BeanUtils;
import org.springframework.boot.context.properties.bind.BindConstructorProvider;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.core.KotlinDetector;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.util.Assert;

/**
 * {@link BindConstructorProvider} used when binding
 * {@link ConfigurationProperties @ConfigurationProperties}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class ConfigurationPropertiesBindConstructorProvider implements BindConstructorProvider {

	static final ConfigurationPropertiesBindConstructorProvider INSTANCE = new ConfigurationPropertiesBindConstructorProvider();

	@Override
	public Constructor<?> getBindConstructor(Bindable<?> bindable, boolean isNestedConstructorBinding) {
		return getBindConstructor(bindable.getType().resolve(), isNestedConstructorBinding);
	}

	/**
	 * 获取与给定类型相关联的构造函数。
	 * <p>
	 * 此方法将尝试查找具有特定注解的构造函数。如果找不到这样的构造函数，并且如果指定类型是构造函数绑定类型或嵌套构造函数绑定为真，则会推断出一个绑定构造函数。
	 * </p>
	 * <p>1. 具有@ConstructorBinding 绑定注解的构造函数（有且只有一个构造函数使用了该注解，且参数 > 0）
	 * 2. 找不到，则如果类型是否为java.lang.Record的子类型 || 给定的类型上注解了{@link ConstructorBinding}。|| isNestedConstructorBinding,
	 * 则如果有且仅有一个参数数量大于0的构造函数，返回该构造函数
	 *
	 * @param type 待查询构造函数的类型。如果类型为null，则返回null。
	 * @param isNestedConstructorBinding 指示是否考虑嵌套构造函数绑定的布尔值。
	 * @return 与给定类型相关联的构造函数，如果没有找到则返回null。
	 */
	Constructor<?> getBindConstructor(Class<?> type, boolean isNestedConstructorBinding) {
		// 检查类型是否为null
		if (type == null) {
			return null;
		}
		// 尝试查找具有@ConstructorBinding 绑定注解的构造函数
		Constructor<?> constructor = findConstructorBindingAnnotatedConstructor(type);
		// 如果没有找到绑定注解的构造函数，
		// 并且类型是构造函数绑定或要求嵌套构造函数绑定，则推断一个绑定构造函数（是否为java.lang.Record的子类型 || 给定的类型上注解了{@link ConstructorBinding}。|| isNestedConstructorBinding）
		if (constructor == null && (isConstructorBindingType(type) || isNestedConstructorBinding)) {
			constructor = deduceBindConstructor(type);
		}
		return constructor;
	}

	private Constructor<?> findConstructorBindingAnnotatedConstructor(Class<?> type) {
		if (isKotlinType(type)) {
			Constructor<?> constructor = BeanUtils.findPrimaryConstructor(type);
			if (constructor != null) {
				return findAnnotatedConstructor(type, constructor);
			}
		}
		return findAnnotatedConstructor(type, type.getDeclaredConstructors());
	}

	/**
	 * 查找带有特定注解的构造函数。
	 *
	 * @param type 待查找构造函数的类类型。
	 * @param candidates 候选构造函数数组。
	 * @return 带有{@link ConstructorBinding}注解的构造函数，如果没有找到则返回null。
	 */
	private Constructor<?> findAnnotatedConstructor(Class<?> type, Constructor<?>... candidates) {
		Constructor<?> constructor = null;

		// 遍历所有候选构造函数
		for (Constructor<?> candidate : candidates) {
			// 检查构造函数是否被@ConstructorBinding注解标记
			if (MergedAnnotations.from(candidate).isPresent(ConstructorBinding.class)) {
				// 确保构造函数有参数
				Assert.state(candidate.getParameterCount() > 0,
						() -> type.getName() + " declares @ConstructorBinding on a no-args constructor");
				// 确保只有一个被@ConstructorBinding注解标记的构造函数
				Assert.state(constructor == null,
						() -> type.getName() + " has more than one @ConstructorBinding constructor");
				constructor = candidate;
			}
		}

		return constructor;
	}

	private boolean isConstructorBindingType(Class<?> type) {
		// 检查类型是否为隐式构造器绑定或构造器绑定注解类型
		return isImplicitConstructorBindingType(type) || isConstructorBindingAnnotatedType(type);
	}

	/**
	 * 检查给定的类型是否为隐式构造函数绑定类型。
	 * 该方法主要用于判断一个类是否是Java记录（Record）类型。
	 *
	 * @param type 待检查的类型。
	 * @return 如果给定类型是Java记录的子类型，则返回true；否则返回false。
	 */
	private boolean isImplicitConstructorBindingType(Class<?> type) {
		// 获取给定类型的父类
		Class<?> superclass = type.getSuperclass();
		// 判断父类是否非空且名为"java.lang.Record"
		return (superclass != null) && "java.lang.Record".equals(superclass.getName());
	}

	/**
	 * 检查给定的类型上是否注解了{@link ConstructorBinding}。
	 * 该方法会搜索类型本身及其继承类型（包括包围类）中是否存在{@link ConstructorBinding}注解。
	 *
	 * @param type 需要检查的类型。
	 * @return 如果类型或其继承类型上存在{@link ConstructorBinding}注解，则返回true；否则返回false。
	 */
	private boolean isConstructorBindingAnnotatedType(Class<?> type) {
		// 通过MergedAnnotations从给定类型及其类型层次结构和包围类中搜索ConstructorBinding注解
		return MergedAnnotations.from(type, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY_AND_ENCLOSING_CLASSES)
				.isPresent(ConstructorBinding.class);
	}

	/**
	 * 推断给定类型的绑定构造函数。
	 * <p>
	 * 此方法首先检查传入的类型是否为Kotlin类型，如果是，则调用 {@code deducedKotlinBindConstructor} 方法推断构造函数。
	 * 如果不是Kotlin类型，则获取该类型的声明构造函数，并检查是否有且仅有一个参数数量大于0的构造函数。
	 * 如果满足条件，则返回该构造函数；否则，返回null。
	 *
	 * @param type 待推断构造函数的类型。
	 * @return 返回推断出的构造函数，如果没有合适的构造函数则返回null。
	 */
	private Constructor<?> deduceBindConstructor(Class<?> type) {
		// 检查是否为Kotlin类型，如果是，则使用专门的方法推断构造函数
		if (isKotlinType(type)) {
			return deducedKotlinBindConstructor(type);
		}
		// 获取并检查类型的声明构造函数
		Constructor<?>[] constructors = type.getDeclaredConstructors();
		if (constructors.length == 1 && constructors[0].getParameterCount() > 0) {
			// 如果有且仅有一个参数数量大于0的构造函数，返回该构造函数
			return constructors[0];
		}
		// 如果没有合适的构造函数，返回null
		return null;
	}

	private Constructor<?> deducedKotlinBindConstructor(Class<?> type) {
		Constructor<?> primaryConstructor = BeanUtils.findPrimaryConstructor(type);
		if (primaryConstructor != null && primaryConstructor.getParameterCount() > 0) {
			return primaryConstructor;
		}
		return null;
	}

	private boolean isKotlinType(Class<?> type) {
		return KotlinDetector.isKotlinPresent() && KotlinDetector.isKotlinType(type);
	}

}
