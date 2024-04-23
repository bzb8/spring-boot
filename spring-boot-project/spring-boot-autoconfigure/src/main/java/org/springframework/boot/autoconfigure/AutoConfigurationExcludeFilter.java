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

package org.springframework.boot.autoconfigure;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;

/**
 * A {@link TypeFilter} implementation that matches registered auto-configuration classes.
 * <p>一个实现{@link TypeFilter}接口的类，用于匹配已注册的自动配置类。
 * 该过滤器帮助确定哪些自动配置类应该被包含或排除在Spring应用程序上下文中。
 *
 * @author Stephane Nicoll
 * @since 1.5.0
 */
public class AutoConfigurationExcludeFilter implements TypeFilter, BeanClassLoaderAware {

	private ClassLoader beanClassLoader;
	// 加载META-INF/spring.factories文件中key为EnableAutoConfiguration.class的value值（实现类）和
	// 按行读取META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports的实现类
	private volatile List<String> autoConfigurations;

	@Override
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}

	// 过滤自动配置类不应该被扫描到
	@Override
	public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory)
			throws IOException {
		// 判断当前类是否有@Configuration注解
		// &&
		// (1. 判断当前类是否有@AutoConfiguration注解
		// ||
	    // 2. META-INF/spring.factories文件中key为EnableAutoConfiguration.class的value值（实现类）和
		// 按行读取META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports的集合中是否包含当前类)
		return isConfiguration(metadataReader) && isAutoConfiguration(metadataReader);
	}

	private boolean isConfiguration(MetadataReader metadataReader) {
		// 判断当前类是否有@Configuration注解
		return metadataReader.getAnnotationMetadata().isAnnotated(Configuration.class.getName());
	}

	/**
	 * 1. 判断当前类是否有@AutoConfiguration注解
	 * 2. META-INF/spring.factories文件中key为EnableAutoConfiguration.class的value值（实现类）和
	 * 按行读取META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports的集合中是否包含当前类
	 *
	 * @param metadataReader
	 * @return
	 */
	private boolean isAutoConfiguration(MetadataReader metadataReader) {
		// 判断当前类是否有@AutoConfiguration注解
		boolean annotatedWithAutoConfiguration = metadataReader.getAnnotationMetadata()
			.isAnnotated(AutoConfiguration.class.getName());
		return annotatedWithAutoConfiguration
				|| getAutoConfigurations().contains(metadataReader.getClassMetadata().getClassName());
	}

	/**
	 * 加载META-INF/spring.factories文件中key为EnableAutoConfiguration.class的value值（实现类）和
	 * 按行读取META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports，并添加到configurations列表中
	 */
	protected List<String> getAutoConfigurations() {
		if (this.autoConfigurations == null) {
			// 加载META-INF/spring.factories文件中的 EnableAutoConfiguration.class的实现类
			List<String> autoConfigurations = new ArrayList<>(
					SpringFactoriesLoader.loadFactoryNames(EnableAutoConfiguration.class, this.beanClassLoader));
			ImportCandidates.load(AutoConfiguration.class, this.beanClassLoader).forEach(autoConfigurations::add);
			this.autoConfigurations = autoConfigurations;
		}
		return this.autoConfigurations;
	}

}
