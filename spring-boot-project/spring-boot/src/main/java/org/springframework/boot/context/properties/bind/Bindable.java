/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.context.properties.bind;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.core.ResolvableType;
import org.springframework.core.style.ToStringCreator;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Source that can be bound by a {@link Binder}.
 * 一个源接口，该接口定义了可以被{@link Binder}绑定的源。
 * 这是一个非常重要的接口，用于在各种上下文中绑定源数据，例如在UI绑定、数据持久化等方面。
 * <p>Bindable封装的是能被Binder进行属性绑定的source（可以指一个类（需要先创建一个instance）、
 * 也可以指一个instance、还可以是instance里的一个需要被绑定的属性），里面包含这个类、instance的Supplier、注解、绑定限制
 *
 * @param <T> the source type
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.0.0
 * @see Bindable#of(Class)
 * @see Bindable#of(ResolvableType)
 */
public final class Bindable<T> {

	private static final Annotation[] NO_ANNOTATIONS = {};

	private static final EnumSet<BindRestriction> NO_BIND_RESTRICTIONS = EnumSet.noneOf(BindRestriction.class);
	// 原始类型
	private final ResolvableType type;
	// box之后的类型
	private final ResolvableType boxedType;
	// 提供该值的工厂对象
	private final Supplier<T> value;
	// 该绑定对象上标注的注解
	private final Annotation[] annotations;
	// 绑定限制
	private final EnumSet<BindRestriction> bindRestrictions;

	private Bindable(ResolvableType type, ResolvableType boxedType, Supplier<T> value, Annotation[] annotations,
			EnumSet<BindRestriction> bindRestrictions) {
		this.type = type;
		this.boxedType = boxedType;
		this.value = value;
		this.annotations = annotations;
		this.bindRestrictions = bindRestrictions;
	}

	/**
	 * Return the type of the item to bind.
	 * @return the type being bound
	 */
	public ResolvableType getType() {
		return this.type;
	}

	/**
	 * Return the boxed type of the item to bind.
	 * @return the boxed type for the item being bound
	 */
	public ResolvableType getBoxedType() {
		return this.boxedType;
	}

	/**
	 * Return a supplier that provides the object value or {@code null}.
	 * @return the value or {@code null}
	 */
	public Supplier<T> getValue() {
		return this.value;
	}

	/**
	 * Return any associated annotations that could affect binding.
	 * @return the associated annotations
	 */
	public Annotation[] getAnnotations() {
		return this.annotations;
	}

	/**
	 * Return a single associated annotations that could affect binding.
	 * @param <A> the annotation type
	 * @param type annotation type
	 * @return the associated annotation or {@code null}
	 */
	@SuppressWarnings("unchecked")
	public <A extends Annotation> A getAnnotation(Class<A> type) {
		for (Annotation annotation : this.annotations) {
			if (type.isInstance(annotation)) {
				return (A) annotation;
			}
		}
		return null;
	}

	/**
	 * Returns {@code true} if the specified bind restriction has been added.
	 * @param bindRestriction the bind restriction to check
	 * @return if the bind restriction has been added
	 * @since 2.5.0
	 */
	public boolean hasBindRestriction(BindRestriction bindRestriction) {
		return this.bindRestrictions.contains(bindRestriction);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		Bindable<?> other = (Bindable<?>) obj;
		boolean result = true;
		result = result && nullSafeEquals(this.type.resolve(), other.type.resolve());
		result = result && nullSafeEquals(this.annotations, other.annotations);
		result = result && nullSafeEquals(this.bindRestrictions, other.bindRestrictions);
		return result;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ObjectUtils.nullSafeHashCode(this.type);
		result = prime * result + ObjectUtils.nullSafeHashCode(this.annotations);
		result = prime * result + ObjectUtils.nullSafeHashCode(this.bindRestrictions);
		return result;
	}

	@Override
	public String toString() {
		ToStringCreator creator = new ToStringCreator(this);
		creator.append("type", this.type);
		creator.append("value", (this.value != null) ? "provided" : "none");
		creator.append("annotations", this.annotations);
		return creator.toString();
	}

	private boolean nullSafeEquals(Object o1, Object o2) {
		return ObjectUtils.nullSafeEquals(o1, o2);
	}

	/**
	 * Create an updated {@link Bindable} instance with the specified annotations.
	 * @param annotations the annotations
	 * @return an updated {@link Bindable}
	 */
	public Bindable<T> withAnnotations(Annotation... annotations) {
		return new Bindable<>(this.type, this.boxedType, this.value,
				(annotations != null) ? annotations : NO_ANNOTATIONS, NO_BIND_RESTRICTIONS);
	}

	/**
	 * Create an updated {@link Bindable} instance with an existing value.
	 * @param existingValue the existing value
	 * @return an updated {@link Bindable}
	 */
	public Bindable<T> withExistingValue(T existingValue) {
		Assert.isTrue(
				existingValue == null || this.type.isArray() || this.boxedType.resolve().isInstance(existingValue),
				() -> "ExistingValue must be an instance of " + this.type);
		Supplier<T> value = (existingValue != null) ? () -> existingValue : null;
		return new Bindable<>(this.type, this.boxedType, value, this.annotations, this.bindRestrictions);
	}

	/**
	 * Create an updated {@link Bindable} instance with a value supplier.
	 * @param suppliedValue the supplier for the value
	 * @return an updated {@link Bindable}
	 */
	public Bindable<T> withSuppliedValue(Supplier<T> suppliedValue) {
		return new Bindable<>(this.type, this.boxedType, suppliedValue, this.annotations, this.bindRestrictions);
	}

	/**
	 * Create an updated {@link Bindable} instance with additional bind restrictions.
	 * @param additionalRestrictions any additional restrictions to apply
	 * @return an updated {@link Bindable}
	 * @since 2.5.0
	 */
	public Bindable<T> withBindRestrictions(BindRestriction... additionalRestrictions) {
		EnumSet<BindRestriction> bindRestrictions = EnumSet.copyOf(this.bindRestrictions);
		bindRestrictions.addAll(Arrays.asList(additionalRestrictions));
		return new Bindable<>(this.type, this.boxedType, this.value, this.annotations, bindRestrictions);
	}

	/**
	 * Create a new {@link Bindable} of the type of the specified instance with an
	 * existing value equal to the instance.
	 * @param <T> the source type
	 * @param instance the instance (must not be {@code null})
	 * @return a {@link Bindable} instance
	 * @see #of(ResolvableType)
	 * @see #withExistingValue(Object)
	 */
	@SuppressWarnings("unchecked")
	public static <T> Bindable<T> ofInstance(T instance) {
		Assert.notNull(instance, "Instance must not be null");
		Class<T> type = (Class<T>) instance.getClass();
		return of(type).withExistingValue(instance);
	}

	/**
	 * Create a new {@link Bindable} of the specified type.
	 * @param <T> the source type
	 * @param type the type (must not be {@code null})
	 * @return a {@link Bindable} instance
	 * @see #of(ResolvableType)
	 */
	public static <T> Bindable<T> of(Class<T> type) {
		Assert.notNull(type, "Type must not be null");
		return of(ResolvableType.forClass(type));
	}

	/**
	 * Create a new {@link Bindable} {@link List} of the specified element type.
	 * @param <E> the element type
	 * @param elementType the list element type
	 * @return a {@link Bindable} instance
	 */
	public static <E> Bindable<List<E>> listOf(Class<E> elementType) {
		return of(ResolvableType.forClassWithGenerics(List.class, elementType));
	}

	/**
	 * Create a new {@link Bindable} {@link Set} of the specified element type.
	 * @param <E> the element type
	 * @param elementType the set element type
	 * @return a {@link Bindable} instance
	 */
	public static <E> Bindable<Set<E>> setOf(Class<E> elementType) {
		return of(ResolvableType.forClassWithGenerics(Set.class, elementType));
	}

	/**
	 * Create a new {@link Bindable} {@link Map} of the specified key and value type.
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param keyType the map key type
	 * @param valueType the map value type
	 * @return a {@link Bindable} instance
	 */
	public static <K, V> Bindable<Map<K, V>> mapOf(Class<K> keyType, Class<V> valueType) {
		return of(ResolvableType.forClassWithGenerics(Map.class, keyType, valueType));
	}

	/**
	 * Create a new {@link Bindable} of the specified type.
	 * 根据指定的类型创建一个新的{@link Bindable}实例。
	 *
	 * <p>这个方法用于生成一个绑定目标，可以将特定类型的值绑定到某个目标上。
	 * 通过传入 {@link ResolvableType} 类型参数，可以动态地处理泛型信息。
	 *
	 * @param <T> the source type
	 * 源类型的泛型参数
	 * @param type the type (must not be {@code null})
	 * 类型信息，必须不为{@code null}。这个参数定义了绑定的目标类型。
	 * @return a {@link Bindable} instance
	 * 一个{@link Bindable}实例，可以用于进一步的绑定操作
	 * @see #of(Class)
	 */
	public static <T> Bindable<T> of(ResolvableType type) {
		// 确保类型参数不为null
		Assert.notNull(type, "Type must not be null");
		// 将类型转换为非原始类型，以支持泛型的正确处理
		ResolvableType boxedType = box(type);
		// 创建并返回一个新的Bindable实例
		return new Bindable<>(type, boxedType, null, NO_ANNOTATIONS, NO_BIND_RESTRICTIONS);
	}

	/**
	 * 将给定的ResolvableType封装到相应的包装器类型中。
	 * 如果类型是基本类型，则将其封装到对应的基本类型包装器中。
	 * 如果类型是数组，则递归地对数组的组件类型进行封装。
	 *
	 * @param type 需要被封装的ResolvableType对象。
	 * @return 封装后的ResolvableType对象。如果输入已经是包装器类型或非基本类型数组，则返回原对象。
	 */
	private static ResolvableType box(ResolvableType type) {
		// 解析type为具体类型，如果type可以解析且为基本类型，则进行封装
		Class<?> resolved = type.resolve();
		if (resolved != null && resolved.isPrimitive()) {
			// 创建一个包含一个元素的该基本类型的数组，用以获取对应的包装器类型
			Object array = Array.newInstance(resolved, 1);
			// 通过数组中元素的类型获取包装器类型
			Class<?> wrapperType = Array.get(array, 0).getClass();
			return ResolvableType.forClass(wrapperType);
		}
		// 如果type可以解析且为数组类型，则递归封装数组的组件类型
		if (resolved != null && resolved.isArray()) {
			return ResolvableType.forArrayComponent(box(type.getComponentType()));
		}
		// 如果type无法解析或无需封装，则直接返回原type
		return type;
	}


	/**
	 * Restrictions that can be applied when binding values.
	 *  枚举类型BindRestriction定义了在绑定值时可以应用的限制条件。
	 *
	 * @since 2.5.0
	 */
	public enum BindRestriction {

		/**
		 * Do not bind direct {@link ConfigurationProperty} matches.
		 * NO_DIRECT_PROPERTY限制条件：避免绑定直接匹配的{@link ConfigurationProperty}。
		 * 不要直接绑定Spring bean，而对其属性进行绑定（直接对bean绑定的话必须要实例化，但bean必须是spring来实例化而不是Binder）
		 */
		NO_DIRECT_PROPERTY

	}

}
