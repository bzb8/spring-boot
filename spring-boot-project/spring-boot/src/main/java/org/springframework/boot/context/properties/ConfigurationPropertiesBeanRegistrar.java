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

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.context.properties.ConfigurationPropertiesBean.BindMethod;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Delegate used by {@link EnableConfigurationPropertiesRegistrar} and
 * {@link ConfigurationPropertiesScanRegistrar} to register a bean definition for a
 * {@link ConfigurationProperties @ConfigurationProperties} class.
 * <p>用于{@link EnableConfigurationPropertiesRegistrar}和{@link ConfigurationPropertiesScanRegistrar}的委托，
 * 用于为{@link ConfigurationProperties @ConfigurationProperties}类注册bean定义。
 * 这个类的作用主要是支持Spring框架中的配置属性绑定功能，允许将配置文件中的属性值绑定到JavaBean上。
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
final class ConfigurationPropertiesBeanRegistrar {

	private final BeanDefinitionRegistry registry;

	private final BeanFactory beanFactory;

	ConfigurationPropertiesBeanRegistrar(BeanDefinitionRegistry registry) {
		this.registry = registry;
		this.beanFactory = (BeanFactory) this.registry;
	}

	/**
	 * 注册给定类型的属性配置。
	 * 该方法通过查找类型上{@link ConfigurationProperties}注解，将类型及其注解信息注册。
	 *
	 * @param type 需要注册的类型，该类型应被{@link ConfigurationProperties}注解标记。
	 * -- @EnableConfigurationProperties注解的value属性值
	 */
	void register(Class<?> type) {
		// 从给定类型及其父类型中查找@ConfigurationProperties注解
		MergedAnnotation<ConfigurationProperties> annotation = MergedAnnotations
				.from(type, SearchStrategy.TYPE_HIERARCHY)
				.get(ConfigurationProperties.class);
		// 使用找到的注解信息注册该类型
		register(type, annotation);
	}

	/**
	 * 注册给定类型的属性配置。
	 * @param type @EnableConfigurationProperties注解的value属性值
	 * @param annotation {@link ConfigurationProperties}注解
	 */
	void register(Class<?> type, MergedAnnotation<ConfigurationProperties> annotation) {
		String name = getName(type, annotation);
		if (!containsBeanDefinition(name)) {
			registerBeanDefinition(name, type, annotation);
		}
	}

	/**
	 * 拼接@ConfigurationProperties注解的prefix属性和type.getName()，得到beanName
	 * @param type 标注@ConfigurationProperties注解的类
	 * @param annotation @ConfigurationProperties注解
	 * @return
	 */
	private String getName(Class<?> type, MergedAnnotation<ConfigurationProperties> annotation) {
		// 获取@ConfigurationProperties注解的prefix属性值
		String prefix = annotation.isPresent() ? annotation.getString("prefix") : "";
		// 拼接prefix和type.getName()，得到beanName
		return (StringUtils.hasText(prefix) ? prefix + "-" + type.getName() : type.getName());
	}

	private boolean containsBeanDefinition(String name) {
		return containsBeanDefinition(this.beanFactory, name);
	}

	private boolean containsBeanDefinition(BeanFactory beanFactory, String name) {
		if (beanFactory instanceof ListableBeanFactory
				&& ((ListableBeanFactory) beanFactory).containsBeanDefinition(name)) {
			return true;
		}
		if (beanFactory instanceof HierarchicalBeanFactory) {
			return containsBeanDefinition(((HierarchicalBeanFactory) beanFactory).getParentBeanFactory(), name);
		}
		return false;
	}

	/**
	 *
	 * @param beanName @ConfigurationProperties注解的prefix属性值 + type.getName()
	 * @param type 标注@ConfigurationProperties注解的类
	 * @param annotation @ConfigurationProperties注解
	 */
	private void registerBeanDefinition(String beanName, Class<?> type,
			MergedAnnotation<ConfigurationProperties> annotation) {
		Assert.state(annotation.isPresent(), () -> "No " + ConfigurationProperties.class.getSimpleName()
				+ " annotation found on  '" + type.getName() + "'.");
		this.registry.registerBeanDefinition(beanName, createBeanDefinition(beanName, type));
	}

	private BeanDefinition createBeanDefinition(String beanName, Class<?> type) {
		BindMethod bindMethod = BindMethod.forType(type);
		RootBeanDefinition definition = new RootBeanDefinition(type);
		// 设置BindMethod属性
		definition.setAttribute(BindMethod.class.getName(), bindMethod);
		if (bindMethod == BindMethod.VALUE_OBJECT) {
			definition.setInstanceSupplier(() -> createValueObject(beanName, type));
		}
		return definition;
	}

	/**
	 * 创建一个值对象，该对象通过配置属性绑定到给定的bean类型和名称。
	 *
	 * @param beanName 要绑定的bean的名称。@ConfigurationProperties注解的prefix属性值 + type.getName()
	 * @param beanType 要绑定的bean的类型。标注@ConfigurationProperties注解的类
	 * @return 绑定后的值对象实例。
	 * @throws ConfigurationPropertiesBindException 如果绑定过程中发生异常。
	 */
	private Object createValueObject(String beanName, Class<?> beanType) {
		// 为给定的bean类型和名称创建一个ConfigurationPropertiesBean实例
		ConfigurationPropertiesBean bean = ConfigurationPropertiesBean.forValueObject(beanType, beanName);

		// 从bean工厂获取ConfigurationPropertiesBinder实例
		ConfigurationPropertiesBinder binder = ConfigurationPropertiesBinder.get(this.beanFactory);

		try {
			// 尝试绑定或创建值对象
			return binder.bindOrCreate(bean);
		}
		catch (Exception ex) {
			// 如果绑定过程中发生异常，抛出ConfigurationPropertiesBindException
			throw new ConfigurationPropertiesBindException(bean, ex);
		}
	}

}
