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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.Aware;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link DeferredImportSelector} to handle {@link EnableAutoConfiguration
 * auto-configuration}. This class can also be subclassed if a custom variant of
 * {@link EnableAutoConfiguration @EnableAutoConfiguration} is needed.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Madhura Bhave
 * @author Moritz Halbritter
 * @since 1.3.0
 * @see EnableAutoConfiguration
 */
public class AutoConfigurationImportSelector implements DeferredImportSelector, BeanClassLoaderAware,
		ResourceLoaderAware, BeanFactoryAware, EnvironmentAware, Ordered {

	private static final AutoConfigurationEntry EMPTY_ENTRY = new AutoConfigurationEntry();

	private static final String[] NO_IMPORTS = {};

	private static final Log logger = LogFactory.getLog(AutoConfigurationImportSelector.class);

	private static final String PROPERTY_NAME_AUTOCONFIGURE_EXCLUDE = "spring.autoconfigure.exclude";

	private ConfigurableListableBeanFactory beanFactory;

	private Environment environment;

	private ClassLoader beanClassLoader;

	private ResourceLoader resourceLoader;

	private ConfigurationClassFilter configurationClassFilter;

	@Override
	public String[] selectImports(AnnotationMetadata annotationMetadata) {
		if (!isEnabled(annotationMetadata)) {
			return NO_IMPORTS;
		}
		AutoConfigurationEntry autoConfigurationEntry = getAutoConfigurationEntry(annotationMetadata);
		return StringUtils.toStringArray(autoConfigurationEntry.getConfigurations());
	}

	@Override
	public Predicate<String> getExclusionFilter() {
		return this::shouldExclude;
	}

	private boolean shouldExclude(String configurationClassName) {
		return getConfigurationClassFilter().filter(Collections.singletonList(configurationClassName)).isEmpty();
	}

	/**
	 * Return the {@link AutoConfigurationEntry} based on the {@link AnnotationMetadata}
	 * of the importing {@link Configuration @Configuration} class.
	 * <p>根据导入的{@link Configuration @Configuration}类的{@link AnnotationMetadata}注解元数据，返回{@link AutoConfigurationEntry}。
	 *
	 * @param annotationMetadata the annotation metadata of the configuration class
	 * 配置类的注解元数据
	 * @return the auto-configurations that should be imported
	 * 应该被导入的自动配置项
	 */
	protected AutoConfigurationEntry getAutoConfigurationEntry(AnnotationMetadata annotationMetadata) {
		// spring.boot.enableautoconfiguration 属性的值为false，则返回空
		if (!isEnabled(annotationMetadata)) {
			return EMPTY_ENTRY;
		}
		AnnotationAttributes attributes = getAttributes(annotationMetadata);
		List<String> configurations = getCandidateConfigurations(annotationMetadata, attributes);
		configurations = removeDuplicates(configurations);
		Set<String> exclusions = getExclusions(annotationMetadata, attributes);
		checkExcludedClasses(configurations, exclusions);
		// 移除排除项
		configurations.removeAll(exclusions);
		// 使用AutoConfigurationImportFilter过滤不匹配的自动配置
		configurations = getConfigurationClassFilter().filter(configurations);

		fireAutoConfigurationImportEvents(configurations, exclusions);
		return new AutoConfigurationEntry(configurations, exclusions);
	}

	@Override
	public Class<? extends Group> getImportGroup() {
		return AutoConfigurationGroup.class;
	}

	/**
	 * spring.boot.enableautoconfiguration 属性的值
	 * @param metadata 配置了@EnableAutoConfiguration注解的类
	 * @return
	 */
	protected boolean isEnabled(AnnotationMetadata metadata) {
		if (getClass() == AutoConfigurationImportSelector.class) {
			// spring.boot.enableautoconfiguration 属性的值
			return getEnvironment().getProperty(EnableAutoConfiguration.ENABLED_OVERRIDE_PROPERTY, Boolean.class, true);
		}
		return true;
	}

	/**
	 * Return the appropriate {@link AnnotationAttributes} from the
	 * {@link AnnotationMetadata}. By default this method will return attributes for
	 * {@link #getAnnotationClass()}.
	 * <p>从注解元数据中获取适当的注解属性。默认情况下，此方法将返回{@link #getAnnotationClass()}的属性。
	 * <p>返回@EnableAutoConfiguration注解的属性
	 * @param metadata the annotation metadata
	 * @return annotation attributes
	 */
	protected AnnotationAttributes getAttributes(AnnotationMetadata metadata) {
		// @EnableAutoConfiguration注解的类名
		String name = getAnnotationClass().getName();
		AnnotationAttributes attributes = AnnotationAttributes.fromMap(metadata.getAnnotationAttributes(name, true));
		Assert.notNull(attributes, () -> "No auto-configuration attributes found. Is " + metadata.getClassName()
				+ " annotated with " + ClassUtils.getShortName(name) + "?");
		return attributes;
	}

	/**
	 * Return the source annotation class used by the selector.
	 * @return the annotation class
	 */
	protected Class<?> getAnnotationClass() {
		return EnableAutoConfiguration.class;
	}

	/**
	 * Return the auto-configuration class names that should be considered. By default
	 * this method will load candidates using {@link ImportCandidates} with
	 * {@link #getSpringFactoriesLoaderFactoryClass()}. For backward compatible reasons it
	 * will also consider {@link SpringFactoriesLoader} with
	 * {@link #getSpringFactoriesLoaderFactoryClass()}.
	 * 获取应考虑的自动配置类名称。默认情况下，此方法将使用{@link ImportCandidates}和
	 * {@link #getSpringFactoriesLoaderFactoryClass()}
	 * 加载候选者。出于向后兼容的原因，它还会考虑使用{@link SpringFactoriesLoader}和{@link #getSpringFactoriesLoaderFactoryClass()}。
	 * <p>加载META-INF/spring.factories文件中key为EnableAutoConfiguration.class的value值（实现类）和
	 * 按行读取META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports，并添加到configurations列表中
	 *
	 * @param metadata the source metadata
	 * 配置类的注解元数据
	 * @param attributes the {@link #getAttributes(AnnotationMetadata) annotation
	 * attributes}
	 * <p>@EnableAutoConfiguration注解的属性值
	 * @return a list of candidate configurations
	 */
	protected List<String> getCandidateConfigurations(AnnotationMetadata metadata, AnnotationAttributes attributes) {
		// 加载META-INF/spring.factories文件中key为EnableAutoConfiguration.class的value值（实现类）
		List<String> configurations = new ArrayList<>(
				SpringFactoriesLoader.loadFactoryNames(getSpringFactoriesLoaderFactoryClass(), getBeanClassLoader()));
		// 按行读取META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports，并添加到configurations列表中
		ImportCandidates.load(AutoConfiguration.class, getBeanClassLoader()).forEach(configurations::add);
		Assert.notEmpty(configurations,
				"No auto configuration classes found in META-INF/spring.factories nor in META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports. If you "
						+ "are using a custom packaging, make sure that file is correct.");
		return configurations;
	}

	/**
	 * Return the class used by {@link SpringFactoriesLoader} to load configuration
	 * candidates.
	 * @return the factory class
	 */
	protected Class<?> getSpringFactoriesLoaderFactoryClass() {
		return EnableAutoConfiguration.class;
	}

	private void checkExcludedClasses(List<String> configurations, Set<String> exclusions) {
		List<String> invalidExcludes = new ArrayList<>(exclusions.size());
		for (String exclusion : exclusions) {
			if (ClassUtils.isPresent(exclusion, getClass().getClassLoader()) && !configurations.contains(exclusion)) {
				invalidExcludes.add(exclusion);
			}
		}
		if (!invalidExcludes.isEmpty()) {
			handleInvalidExcludes(invalidExcludes);
		}
	}

	/**
	 * Handle any invalid excludes that have been specified.
	 * @param invalidExcludes the list of invalid excludes (will always have at least one
	 * element)
	 */
	protected void handleInvalidExcludes(List<String> invalidExcludes) {
		StringBuilder message = new StringBuilder();
		for (String exclude : invalidExcludes) {
			message.append("\t- ").append(exclude).append(String.format("%n"));
		}
		throw new IllegalStateException(String.format(
				"The following classes could not be excluded because they are not auto-configuration classes:%n%s",
				message));
	}

	/**
	 * Return any exclusions that limit the candidate configurations.
	 * @param metadata the source metadata
	 * @param attributes the {@link #getAttributes(AnnotationMetadata) annotation
	 * attributes}
	 * @return exclusions or an empty set
	 * 从@EnableAutoConfiguration注解的exclude和excludeName属性中获取排除的配置类
	 */
	protected Set<String> getExclusions(AnnotationMetadata metadata, AnnotationAttributes attributes) {
		Set<String> excluded = new LinkedHashSet<>();
		excluded.addAll(asList(attributes, "exclude"));
		excluded.addAll(asList(attributes, "excludeName"));
		excluded.addAll(getExcludeAutoConfigurationsProperty());
		return excluded;
	}

	/**
	 * Returns the auto-configurations excluded by the
	 * {@code spring.autoconfigure.exclude} property.
	 * @return excluded auto-configurations
	 * 返回spring.autoconfigure.exclude属性的值
	 * @since 2.3.2
	 */
	protected List<String> getExcludeAutoConfigurationsProperty() {
		Environment environment = getEnvironment();
		if (environment == null) {
			return Collections.emptyList();
		}
		if (environment instanceof ConfigurableEnvironment) {
			Binder binder = Binder.get(environment);
			return binder.bind(PROPERTY_NAME_AUTOCONFIGURE_EXCLUDE, String[].class)
				.map(Arrays::asList)
				.orElse(Collections.emptyList());
		}
		String[] excludes = environment.getProperty(PROPERTY_NAME_AUTOCONFIGURE_EXCLUDE, String[].class);
		return (excludes != null) ? Arrays.asList(excludes) : Collections.emptyList();
	}

	protected List<AutoConfigurationImportFilter> getAutoConfigurationImportFilters() {
		// 加载META-INF/spring.factories文件中key为AutoConfigurationImportFilter.class的value值（实现类）
		// org.springframework.boot.autoconfigure.condition.OnBeanCondition
		// org.springframework.boot.autoconfigure.condition.OnClassCondition
		// org.springframework.boot.autoconfigure.condition.OnWebApplicationCondition
		return SpringFactoriesLoader.loadFactories(AutoConfigurationImportFilter.class, this.beanClassLoader);
	}

	private ConfigurationClassFilter getConfigurationClassFilter() {
		if (this.configurationClassFilter == null) {
			List<AutoConfigurationImportFilter> filters = getAutoConfigurationImportFilters();
			for (AutoConfigurationImportFilter filter : filters) {
				invokeAwareMethods(filter);
			}
			this.configurationClassFilter = new ConfigurationClassFilter(this.beanClassLoader, filters);
		}
		return this.configurationClassFilter;
	}

	protected final <T> List<T> removeDuplicates(List<T> list) {
		return new ArrayList<>(new LinkedHashSet<>(list));
	}

	protected final List<String> asList(AnnotationAttributes attributes, String name) {
		String[] value = attributes.getStringArray(name);
		return Arrays.asList(value);
	}

	private void fireAutoConfigurationImportEvents(List<String> configurations, Set<String> exclusions) {
		List<AutoConfigurationImportListener> listeners = getAutoConfigurationImportListeners();
		if (!listeners.isEmpty()) {
			AutoConfigurationImportEvent event = new AutoConfigurationImportEvent(this, configurations, exclusions);
			for (AutoConfigurationImportListener listener : listeners) {
				invokeAwareMethods(listener);
				listener.onAutoConfigurationImportEvent(event);
			}
		}
	}

	protected List<AutoConfigurationImportListener> getAutoConfigurationImportListeners() {
		// 加载META-INF/spring.factories文件中key为AutoConfigurationImportListener.class的value值（实现类）
		return SpringFactoriesLoader.loadFactories(AutoConfigurationImportListener.class, this.beanClassLoader);
	}

	private void invokeAwareMethods(Object instance) {
		if (instance instanceof Aware) {
			if (instance instanceof BeanClassLoaderAware) {
				((BeanClassLoaderAware) instance).setBeanClassLoader(this.beanClassLoader);
			}
			if (instance instanceof BeanFactoryAware) {
				((BeanFactoryAware) instance).setBeanFactory(this.beanFactory);
			}
			if (instance instanceof EnvironmentAware) {
				((EnvironmentAware) instance).setEnvironment(this.environment);
			}
			if (instance instanceof ResourceLoaderAware) {
				((ResourceLoaderAware) instance).setResourceLoader(this.resourceLoader);
			}
		}
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		Assert.isInstanceOf(ConfigurableListableBeanFactory.class, beanFactory);
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
	}

	protected final ConfigurableListableBeanFactory getBeanFactory() {
		return this.beanFactory;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	protected ClassLoader getBeanClassLoader() {
		return this.beanClassLoader;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	protected final Environment getEnvironment() {
		return this.environment;
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	protected final ResourceLoader getResourceLoader() {
		return this.resourceLoader;
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE - 1;
	}

	private static class ConfigurationClassFilter {
		// PropertiesAutoConfigurationMetadata
		private final AutoConfigurationMetadata autoConfigurationMetadata;

		/**
		 * 加载META-INF/spring.factories文件中key为AutoConfigurationImportFilter.class的value值（实现类）
		 * org.springframework.boot.autoconfigure.condition.OnBeanCondition
		 * org.springframework.boot.autoconfigure.condition.OnClassCondition
		 * org.springframework.boot.autoconfigure.condition.OnWebApplicationCondition
 		 */
		private final List<AutoConfigurationImportFilter> filters;

		ConfigurationClassFilter(ClassLoader classLoader, List<AutoConfigurationImportFilter> filters) {
			this.autoConfigurationMetadata = AutoConfigurationMetadataLoader.loadMetadata(classLoader);
			this.filters = filters;
		}

		List<String> filter(List<String> configurations) {
			long startTime = System.nanoTime();
			String[] candidates = StringUtils.toStringArray(configurations);
			boolean skipped = false;
			for (AutoConfigurationImportFilter filter : this.filters) {
				// 使用AutoConfigurationImportFilter过滤不匹配的配置类
				boolean[] match = filter.match(candidates, this.autoConfigurationMetadata);
				for (int i = 0; i < match.length; i++) {
					if (!match[i]) {
						candidates[i] = null;
						skipped = true;
					}
				}
			}
			if (!skipped) {
				return configurations;
			}
			List<String> result = new ArrayList<>(candidates.length);
			for (String candidate : candidates) {
				if (candidate != null) {
					result.add(candidate);
				}
			}
			if (logger.isTraceEnabled()) {
				int numberFiltered = configurations.size() - result.size();
				logger.trace("Filtered " + numberFiltered + " auto configuration class in "
						+ TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime) + " ms");
			}
			return result;
		}

	}

	private static class AutoConfigurationGroup
			implements DeferredImportSelector.Group, BeanClassLoaderAware, BeanFactoryAware, ResourceLoaderAware {
		// 待配置的自动配置类 -> 配置类的元数据
		// 待配置的自动配置类
		// 加载META-INF/spring.factories文件中key为EnableAutoConfiguration.class的value值（实现类）和
		// 按行读取META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports，并添加到configurations列表中
		// 过滤之后的
		private final Map<String, AnnotationMetadata> entries = new LinkedHashMap<>();
		// process()方法处理过后缓存的AutoConfigurationEntry
		private final List<AutoConfigurationEntry> autoConfigurationEntries = new ArrayList<>();

		private ClassLoader beanClassLoader;

		private BeanFactory beanFactory;

		private ResourceLoader resourceLoader;
		// PropertiesAutoConfigurationMetadata
		private AutoConfigurationMetadata autoConfigurationMetadata;

		@Override
		public void setBeanClassLoader(ClassLoader classLoader) {
			this.beanClassLoader = classLoader;
		}

		@Override
		public void setBeanFactory(BeanFactory beanFactory) {
			this.beanFactory = beanFactory;
		}

		@Override
		public void setResourceLoader(ResourceLoader resourceLoader) {
			this.resourceLoader = resourceLoader;
		}

		@Override
		public void process(AnnotationMetadata annotationMetadata, DeferredImportSelector deferredImportSelector) {
			Assert.state(deferredImportSelector instanceof AutoConfigurationImportSelector,
					() -> String.format("Only %s implementations are supported, got %s",
							AutoConfigurationImportSelector.class.getSimpleName(),
							deferredImportSelector.getClass().getName()));
			AutoConfigurationEntry autoConfigurationEntry = ((AutoConfigurationImportSelector) deferredImportSelector)
				.getAutoConfigurationEntry(annotationMetadata);
			this.autoConfigurationEntries.add(autoConfigurationEntry);
			for (String importClassName : autoConfigurationEntry.getConfigurations()) {
				this.entries.putIfAbsent(importClassName, annotationMetadata);
			}
		}

		@Override
		public Iterable<Entry> selectImports() {
			if (this.autoConfigurationEntries.isEmpty()) {
				return Collections.emptyList();
			}
			// 获取所有排除的配置类
			Set<String> allExclusions = this.autoConfigurationEntries.stream()
				.map(AutoConfigurationEntry::getExclusions)
				.flatMap(Collection::stream)
				.collect(Collectors.toSet());
			// 获取所有配置类
			Set<String> processedConfigurations = this.autoConfigurationEntries.stream()
				.map(AutoConfigurationEntry::getConfigurations)
				.flatMap(Collection::stream)
				.collect(Collectors.toCollection(LinkedHashSet::new));
			// 排除
			processedConfigurations.removeAll(allExclusions);

			return sortAutoConfigurations(processedConfigurations, getAutoConfigurationMetadata()).stream()
				.map((importClassName) -> new Entry(this.entries.get(importClassName), importClassName))
				.collect(Collectors.toList()); // 获取排序后的配置类
		}

		private AutoConfigurationMetadata getAutoConfigurationMetadata() {
			if (this.autoConfigurationMetadata == null) {
				this.autoConfigurationMetadata = AutoConfigurationMetadataLoader.loadMetadata(this.beanClassLoader);
			}
			return this.autoConfigurationMetadata;
		}

		/**
		 *
		 * @param configurations
		 * 1. META-INF/spring.factories文件中key为EnableAutoConfiguration.class的value值（实现类）
		 * 2. 按行读取META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
		 * 3. 过滤从@EnableAutoConfiguration注解的exclude和excludeName属性中获取排除的配置类
		 * 4. 加载META-INF/spring.factories文件中key为AutoConfigurationImportFilter.class的value值（实现类）
		 * org.springframework.boot.autoconfigure.condition.OnBeanCondition
		 * org.springframework.boot.autoconfigure.condition.OnClassCondition
		 * org.springframework.boot.autoconfigure.condition.OnWebApplicationCondition，调用它的filter方法过滤
		 *
		 * @param autoConfigurationMetadata
		 * @return
		 */
		private List<String> sortAutoConfigurations(Set<String> configurations,
				AutoConfigurationMetadata autoConfigurationMetadata) {
			return new AutoConfigurationSorter(getMetadataReaderFactory(), autoConfigurationMetadata)
				.getInPriorityOrder(configurations);
		}

		private MetadataReaderFactory getMetadataReaderFactory() {
			try {
				// org.springframework.boot.autoconfigure.internalCachingMetadataReaderFactory的bean
				return this.beanFactory.getBean(SharedMetadataReaderFactoryContextInitializer.BEAN_NAME,
						MetadataReaderFactory.class);
			}
			catch (NoSuchBeanDefinitionException ex) {
				return new CachingMetadataReaderFactory(this.resourceLoader);
			}
		}

	}

	protected static class AutoConfigurationEntry {
		// 待配置的自动配置类
		// 加载META-INF/spring.factories文件中key为EnableAutoConfiguration.class的value值（实现类）和
		// 按行读取META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports，并添加到configurations列表中
		// 过滤之后的
		private final List<String> configurations;
		// 排除的配置类，从@EnableAutoConfiguration注解的exclude和excludeName属性中获取排除的配置类
		private final Set<String> exclusions;

		private AutoConfigurationEntry() {
			this.configurations = Collections.emptyList();
			this.exclusions = Collections.emptySet();
		}

		/**
		 * Create an entry with the configurations that were contributed and their
		 * exclusions.
		 * @param configurations the configurations that should be imported
		 * @param exclusions the exclusions that were applied to the original list
		 */
		AutoConfigurationEntry(Collection<String> configurations, Collection<String> exclusions) {
			this.configurations = new ArrayList<>(configurations);
			this.exclusions = new HashSet<>(exclusions);
		}

		public List<String> getConfigurations() {
			return this.configurations;
		}

		public Set<String> getExclusions() {
			return this.exclusions;
		}

	}

}
