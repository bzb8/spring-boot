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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.Assert;

/**
 * Sort {@link EnableAutoConfiguration auto-configuration} classes into priority order by
 * reading {@link AutoConfigureOrder @AutoConfigureOrder},
 * {@link AutoConfigureBefore @AutoConfigureBefore} and
 * {@link AutoConfigureAfter @AutoConfigureAfter} annotations (without loading classes).
 *
 * @author Phillip Webb
 */
class AutoConfigurationSorter {
	// beanName为org.springframework.boot.autoconfigure.internalCachingMetadataReaderFactory的bean的bean
	private final MetadataReaderFactory metadataReaderFactory;
	// PropertiesAutoConfigurationMetadata
	private final AutoConfigurationMetadata autoConfigurationMetadata;

	AutoConfigurationSorter(MetadataReaderFactory metadataReaderFactory,
			AutoConfigurationMetadata autoConfigurationMetadata) {
		Assert.notNull(metadataReaderFactory, "MetadataReaderFactory must not be null");
		this.metadataReaderFactory = metadataReaderFactory;
		this.autoConfigurationMetadata = autoConfigurationMetadata;
	}

	/**
	 * 按优先级顺序获取配置类名称的列表。
	 * 首先按字母顺序排序，然后按配置的顺序排序，最后考虑@AutoConfigureBefore和@AutoConfigureAfter注解的影响。
	 *
	 * @param classNames 配置类的名称集合，它们将被按优先级顺序排列。
	 * 1. META-INF/spring.factories文件中key为EnableAutoConfiguration.class的value值（实现类）
	 * 2. 按行读取META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
	 * 3. 过滤从@EnableAutoConfiguration注解的exclude和excludeName属性中获取排除的配置类
	 * 4. 加载META-INF/spring.factories文件中key为AutoConfigurationImportFilter.class的value值（实现类）
	 * org.springframework.boot.autoconfigure.condition.OnBeanCondition
	 * org.springframework.boot.autoconfigure.condition.OnClassCondition
	 * org.springframework.boot.autoconfigure.condition.OnWebApplicationCondition，调用它的filter方法过滤
	 *
	 * @return 排序后的配置类名称列表。
	 */
	List<String> getInPriorityOrder(Collection<String> classNames) {
		// 初始化自动配置类信息，包括从@EnableAutoConfiguration注解中解析出的配置类、排除的配置类等
		AutoConfigurationClasses classes = new AutoConfigurationClasses(this.metadataReaderFactory,
				this.autoConfigurationMetadata, classNames);
		List<String> orderedClassNames = new ArrayList<>(classNames);
		// Initially sort alphabetically 首先按字母顺序对配置类名称进行排序
		Collections.sort(orderedClassNames);
		// Then sort by order  然后按照配置类中定义的顺序进行排序,获取当前自动配置类上的@AutoConfigureOrder注解的value属性值排序
		orderedClassNames.sort((o1, o2) -> {
			int i1 = classes.get(o1).getOrder();
			int i2 = classes.get(o2).getOrder();
			return Integer.compare(i1, i2);
		});
		// Then respect @AutoConfigureBefore @AutoConfigureAfter 最后，考虑@AutoConfigureBefore和@AutoConfigureAfter注解的影响来调整排序顺序
		orderedClassNames = sortByAnnotation(classes, orderedClassNames);
		return orderedClassNames;
	}

	private List<String> sortByAnnotation(AutoConfigurationClasses classes, List<String> classNames) {
		List<String> toSort = new ArrayList<>(classNames); // 初始的自动配置类
		toSort.addAll(classes.getAllNames()); // 包含它们依赖的所有自动配置类, 初始的自动配置类重复了
		Set<String> sorted = new LinkedHashSet<>();
		Set<String> processing = new LinkedHashSet<>();
		while (!toSort.isEmpty()) {
			doSortByAfterAnnotation(classes, toSort, sorted, processing, null);
		}
		sorted.retainAll(classNames);
		return new ArrayList<>(sorted);
	}

	private void doSortByAfterAnnotation(AutoConfigurationClasses classes, List<String> toSort, Set<String> sorted,
			Set<String> processing, String current) {
		if (current == null) { // 如果当前处理的类名称为空，则从待排序列表中取出第一个类作为当前处理的类
			current = toSort.remove(0);
		}
		processing.add(current); // 将当前处理的类加入处理中的集合
		for (String after : classes.getClassesRequestedAfter(current)) {  // 遍历当前类之后依赖的所有类
			checkForCycles(processing, current, after);
			if (!sorted.contains(after) && toSort.contains(after)) {
				doSortByAfterAnnotation(classes, toSort, sorted, processing, after); // 递归处理当前类之后依赖的所有类
			}
		}
		processing.remove(current);  // 从处理中的集合中移除当前类，表示该类处理完成
		sorted.add(current); // 将当前类加入已排序的集合
	}

	private void checkForCycles(Set<String> processing, String current, String after) {
		Assert.state(!processing.contains(after),
				() -> "AutoConfigure cycle detected between " + current + " and " + after);
	}

	private static class AutoConfigurationClasses {
		// 待导入的自动配置类的名称 -> AutoConfigurationClass，包含它依赖的@AutoConfigureBefore和@AutoConfigureAfter配置类
		private final Map<String, AutoConfigurationClass> classes = new HashMap<>();

		AutoConfigurationClasses(MetadataReaderFactory metadataReaderFactory,
				AutoConfigurationMetadata autoConfigurationMetadata, Collection<String> classNames) {
			addToClasses(metadataReaderFactory, autoConfigurationMetadata, classNames, true);
		}

		Set<String> getAllNames() {
			return this.classes.keySet();
		}

		/**
		 *
		 * @param metadataReaderFactory
		 * @param autoConfigurationMetadata
		 * @param classNames 待导入的自动配置类名称列表
		 * @param required
		 */
		private void addToClasses(MetadataReaderFactory metadataReaderFactory,
				AutoConfigurationMetadata autoConfigurationMetadata, Collection<String> classNames, boolean required) {
			for (String className : classNames) {
				if (!this.classes.containsKey(className)) {
					AutoConfigurationClass autoConfigurationClass = new AutoConfigurationClass(className,
							metadataReaderFactory, autoConfigurationMetadata);
					boolean available = autoConfigurationClass.isAvailable();
					if (required || available) {
						this.classes.put(className, autoConfigurationClass);
					}
					if (available) { // 递归获取当前自动配置类依赖的@AutoConfigureBefore和@AutoConfigureAfter配置类，并加入到classes中
						addToClasses(metadataReaderFactory, autoConfigurationMetadata,
								autoConfigurationClass.getBefore(), false);
						addToClasses(metadataReaderFactory, autoConfigurationMetadata,
								autoConfigurationClass.getAfter(), false);
					}
				}
			}
		}

		AutoConfigurationClass get(String className) {
			return this.classes.get(className);
		}

		Set<String> getClassesRequestedAfter(String className) { // 取在指定类之后请求的所有类的集合。
			Set<String> classesRequestedAfter = new LinkedHashSet<>(get(className).getAfter()); // 首先从get方法中获取到在className之后的所有类，并放入LinkedHashSet中，以保持顺序
			this.classes.forEach((name, autoConfigurationClass) -> { // 遍历所有类，将所有在指定类之前被请求的类添加到classesRequestedAfter集合中
				if (autoConfigurationClass.getBefore().contains(className)) {
					classesRequestedAfter.add(name);
				}
			});
			return classesRequestedAfter;
		}

	}

	private static class AutoConfigurationClass {
		// 待导入的自动配置类名称
		private final String className;
		// beanName为org.springframework.boot.autoconfigure.internalCachingMetadataReaderFactory的bean的bean
		private final MetadataReaderFactory metadataReaderFactory;
		// PropertiesAutoConfigurationMetadata
		private final AutoConfigurationMetadata autoConfigurationMetadata;
		// 待导入的自动配置类的注解元数据
		private volatile AnnotationMetadata annotationMetadata;
		// 获取当前待导入的自动配置类的@AutoConfigureBefore的"value"和"name"的属性值
		private volatile Set<String> before;
		// 获取当前待导入的自动配置类的@AutoConfigureAfter的"value"和"name"的属性值
		private volatile Set<String> after;

		AutoConfigurationClass(String className, MetadataReaderFactory metadataReaderFactory,
				AutoConfigurationMetadata autoConfigurationMetadata) {
			this.className = className;
			this.metadataReaderFactory = metadataReaderFactory;
			this.autoConfigurationMetadata = autoConfigurationMetadata;
		}

		boolean isAvailable() {
			try {
				if (!wasProcessed()) {
					getAnnotationMetadata();
				}
				return true;
			}
			catch (Exception ex) {
				return false;
			}
		}

		Set<String> getBefore() {
			if (this.before == null) {
				this.before = (wasProcessed() ? this.autoConfigurationMetadata.getSet(this.className,
						"AutoConfigureBefore", Collections.emptySet()) : getAnnotationValue(AutoConfigureBefore.class));
			}
			return this.before;
		}

		Set<String> getAfter() {
			if (this.after == null) {
				this.after = (wasProcessed() ? this.autoConfigurationMetadata.getSet(this.className,
						"AutoConfigureAfter", Collections.emptySet()) : getAnnotationValue(AutoConfigureAfter.class));
			}
			return this.after;
		}

		private int getOrder() {
			if (wasProcessed()) {
				return this.autoConfigurationMetadata.getInteger(this.className, "AutoConfigureOrder",
						AutoConfigureOrder.DEFAULT_ORDER);
			}
			Map<String, Object> attributes = getAnnotationMetadata()
				.getAnnotationAttributes(AutoConfigureOrder.class.getName()); // 获取当前自动配置类上的@AutoConfigureOrder注解的value属性值
			return (attributes != null) ? (Integer) attributes.get("value") : AutoConfigureOrder.DEFAULT_ORDER;
		}

		private boolean wasProcessed() { // META-INF/spring-autoconfigure-metadata.properties文件是否包含该className的key
			return (this.autoConfigurationMetadata != null
					&& this.autoConfigurationMetadata.wasProcessed(this.className));
		}

		private Set<String> getAnnotationValue(Class<?> annotation) { // 获取指定注解的value和name属性值
			Map<String, Object> attributes = getAnnotationMetadata().getAnnotationAttributes(annotation.getName(),
					true); // 获取指定注解的所有属性值
			if (attributes == null) {
				return Collections.emptySet();
			}
			Set<String> value = new LinkedHashSet<>();
			Collections.addAll(value, (String[]) attributes.get("value"));
			Collections.addAll(value, (String[]) attributes.get("name")); // 将注解中名为"value"和"name"的属性值添加到集合中
			return value;
		}

		private AnnotationMetadata getAnnotationMetadata() {
			if (this.annotationMetadata == null) {
				try {
					MetadataReader metadataReader = this.metadataReaderFactory.getMetadataReader(this.className);
					this.annotationMetadata = metadataReader.getAnnotationMetadata();
				}
				catch (IOException ex) {
					throw new IllegalStateException("Unable to read meta-data for class " + this.className, ex);
				}
			}
			return this.annotationMetadata;
		}

	}

}
