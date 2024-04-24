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

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.validation.beanvalidation.MethodValidationExcludeFilter;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.Conventions;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.type.AnnotationMetadata;

/**
 * {@link ImportBeanDefinitionRegistrar} for
 * {@link EnableConfigurationProperties @EnableConfigurationProperties}.
 * 用于{@link EnableConfigurationProperties @EnableConfigurationProperties}的{@link ImportBeanDefinitionRegistrar}。
 * <p>这个类实现了ImportBeanDefinitionRegistrar接口，主要功能是在应用启用@EnableConfigurationProperties注解时，
 * 注册Bean定义，将属性文件中的配置绑定到Bean上。
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class EnableConfigurationPropertiesRegistrar implements ImportBeanDefinitionRegistrar {

	private static final String METHOD_VALIDATION_EXCLUDE_FILTER_BEAN_NAME = Conventions
		.getQualifiedAttributeName(EnableConfigurationPropertiesRegistrar.class, "methodValidationExcludeFilter");

	/**
	 *
	 * @param metadata annotation metadata of the importing class
	 * <p>当前配置类的注解元数据
	 * @param registry current bean definition registry
	 */
	@Override
	public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
		registerInfrastructureBeans(registry);
		registerMethodValidationExcludeFilter(registry);
		ConfigurationPropertiesBeanRegistrar beanRegistrar = new ConfigurationPropertiesBeanRegistrar(registry);
		getTypes(metadata).forEach(beanRegistrar::register);
	}

	/**
	 * 从给定的元数据中获取配置属性类型集合。
	 * 这个方法主要用于解析被{@link EnableConfigurationProperties}注解标注的类，
	 * 并收集这些注解中指定的属性类型。
	 * <p>获取当前配置类的@EnableConfigurationProperties的value属性值，并过滤掉void类型的属性类型。
	 *
	 * @param metadata 元数据对象，用于解析注解信息。
	 * @return 集合，包含所有非void类型的配置属性类。
	 */
	private Set<Class<?>> getTypes(AnnotationMetadata metadata) {
		// 从元数据中流式读取所有EnableConfigurationProperties注解
		return metadata.getAnnotations()
				.stream(EnableConfigurationProperties.class)
				// 扁平化处理，将注解中class数组转换为流
				.flatMap((annotation) -> Arrays.stream(annotation.getClassArray(MergedAnnotation.VALUE)))
				// 过滤掉类型为void的类
				.filter((type) -> void.class != type)
				// 收集所有非void类型的类到集合中
				.collect(Collectors.toSet());
	}

	/**
	 * 注册基础组件
	 * ConfigurationPropertiesBindingPostProcessor
	 * ConfigurationPropertiesBinder.Factory
	 * ConfigurationPropertiesBinder
	 * BoundConfigurationProperties
	 * @param registry
	 */
	static void registerInfrastructureBeans(BeanDefinitionRegistry registry) {
		ConfigurationPropertiesBindingPostProcessor.register(registry);
		BoundConfigurationProperties.register(registry);
	}

	static void registerMethodValidationExcludeFilter(BeanDefinitionRegistry registry) {
		// 注册MethodValidationExcludeFilter类型的bean，判断给定类的是否使用了@ConfigurationProperties注解，
		if (!registry.containsBeanDefinition(METHOD_VALIDATION_EXCLUDE_FILTER_BEAN_NAME)) {
			BeanDefinition definition = BeanDefinitionBuilder
				.genericBeanDefinition(MethodValidationExcludeFilter.class,
						() -> MethodValidationExcludeFilter.byAnnotation(ConfigurationProperties.class))
				.setRole(BeanDefinition.ROLE_INFRASTRUCTURE)
				.getBeanDefinition();
			registry.registerBeanDefinition(METHOD_VALIDATION_EXCLUDE_FILTER_BEAN_NAME, definition);
		}
	}

}
