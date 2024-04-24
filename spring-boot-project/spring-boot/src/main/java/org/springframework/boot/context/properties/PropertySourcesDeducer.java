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

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySources;
import org.springframework.util.Assert;

/**
 * Utility to deduce the {@link PropertySources} to use for configuration binding.
 * 配置绑定用属性源推断工具类。
 * 本类提供功能，用于从系统属性、环境变量、外部配置文件等多种来源识别并收集相关属性源。
 * 这些属性源随后可被配置绑定机制用于将属性映射到配置对象。
 *
 * @author Phillip Webb
 */
class PropertySourcesDeducer {

	private static final Log logger = LogFactory.getLog(PropertySourcesDeducer.class);

	private final ApplicationContext applicationContext;

	PropertySourcesDeducer(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	/**
	 * 获取属性源。
	 * 这个方法首先尝试从 PropertySourcesPlaceholderConfigurer 中获取已应用的属性源。
	 * 如果 PropertySourcesPlaceholderConfigurer 存在但未找到任何属性源，
	 * 则从环境变量中提取属性源。
	 *
	 * @return PropertySources 返回找到的属性源。如果既不能从 PropertySourcesPlaceholderConfigurer 获取，
	 *         也不能从环境变量中提取到属性源，则抛出异常。
	 */
	PropertySources getPropertySources() {
		// 尝试从 PropertySourcesPlaceholderConfigurer 获取已应用的属性源
		PropertySourcesPlaceholderConfigurer configurer = getSinglePropertySourcesPlaceholderConfigurer();
		if (configurer != null) {
			// 如果 configurer 非空，返回其应用的属性源
			return configurer.getAppliedPropertySources();
		}
		// 从环境变量中提取属性源
		MutablePropertySources sources = extractEnvironmentPropertySources();
		Assert.state(sources != null,
				"Unable to obtain PropertySources from PropertySourcesPlaceholderConfigurer or Environment");
		// 返回提取到的属性源
		return sources;
	}

	/**
	 * 获取单个 PropertySourcesPlaceholderConfigurer 实例。
	 * 此方法旨在避免过早实例化所有 FactoryBeans，同时尝试从应用上下文中获取单个 PropertySourcesPlaceholderConfigurer 实例。
	 * 如果存在多个该类型的实例，将会记录警告信息并返回 null。
	 *
	 * @return 如果找到单个 PropertySourcesPlaceholderConfigurer 实例，则返回该实例；如果找到多个实例或没有找到实例，返回 null。
	 */
	private PropertySourcesPlaceholderConfigurer getSinglePropertySourcesPlaceholderConfigurer() {
		// 小心避免触发所有 FactoryBeans 的早期实例化
		Map<String, PropertySourcesPlaceholderConfigurer> beans = this.applicationContext
				.getBeansOfType(PropertySourcesPlaceholderConfigurer.class, false, false);
		if (beans.size() == 1) {
			// 如果找到单个 PropertySourcesPlaceholderConfigurer 实例，返回该实例
			return beans.values().iterator().next();
		}
		if (beans.size() > 1 && logger.isWarnEnabled()) {
			// 如果找到多个 PropertySourcesPlaceholderConfigurer 实例，记录警告信息
			logger.warn("Multiple PropertySourcesPlaceholderConfigurer beans registered " + beans.keySet()
					+ ", falling back to Environment");
		}
		// 如果找到零个或多个实例，返回 null
		return null;
	}

	private MutablePropertySources extractEnvironmentPropertySources() {
		Environment environment = this.applicationContext.getEnvironment();
		if (environment instanceof ConfigurableEnvironment) {
			return ((ConfigurableEnvironment) environment).getPropertySources();
		}
		return null;
	}

}
