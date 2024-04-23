/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.context;

import java.io.IOException;
import java.util.Collection;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;

/**
 * Provides exclusion {@link TypeFilter TypeFilters} that are loaded from the
 * {@link BeanFactory} and automatically applied to {@code SpringBootApplication}
 * scanning. Can also be used directly with {@code @ComponentScan} as follows:
 * <p>提供从{@link BeanFactory}加载的排除{@link TypeFilter TypeFilters}，并自动应用于{@code SpringBootApplication}扫描。
 * 也可以直接与{@code @ComponentScan}一起使用，如下所示：
 * <pre class="code">
 * &#064;ComponentScan(excludeFilters = @Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class))
 * </pre>
 * <p>
 * Implementations should provide a subclass registered with {@link BeanFactory} and
 * override the {@link #match(MetadataReader, MetadataReaderFactory)} method. They should
 * also implement a valid {@link #hashCode() hashCode} and {@link #equals(Object) equals}
 * methods so that they can be used as part of Spring test's application context caches.
 * <p>
 * 实现类应提供一个注册到{@link BeanFactory}的子类，并重写{@link #match(MetadataReader, MetadataReaderFactory)}方法。
 * 它们还应该实现有效的{@link #hashCode() hashCode}和{@link #equals(Object) equals}方法，以便作为Spring测试的应用上下文缓存的一部分使用。
 *
 * <p>
 * Note that {@code TypeExcludeFilters} are initialized very early in the application
 * lifecycle, they should generally not have dependencies on any other beans. They are
 * primarily used internally to support {@code spring-boot-test}.
 * 请注意，{@code TypeExcludeFilters}在应用程序生命周期的非常早期阶段进行初始化，通常不应依赖于任何其他bean。
 * 它们主要用于支持{@code spring-boot-test}。
 *
 * @author Phillip Webb
 * @since 1.4.0
 */
public class TypeExcludeFilter implements TypeFilter, BeanFactoryAware {

	private BeanFactory beanFactory;

	/**
	 * spring容器中类型为TypeExcludeFilter的bean集合
	 */
	private Collection<TypeExcludeFilter> delegates;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory)
			throws IOException {
		if (this.beanFactory instanceof ListableBeanFactory && getClass() == TypeExcludeFilter.class) {
			for (TypeExcludeFilter delegate : getDelegates()) {
				if (delegate.match(metadataReader, metadataReaderFactory)) {
					return true;
				}
			}
		}
		return false;
	}

	private Collection<TypeExcludeFilter> getDelegates() {
		Collection<TypeExcludeFilter> delegates = this.delegates;
		if (delegates == null) {
			delegates = ((ListableBeanFactory) this.beanFactory).getBeansOfType(TypeExcludeFilter.class).values();
			this.delegates = delegates;
		}
		return delegates;
	}

	@Override
	public boolean equals(Object obj) {
		throw new IllegalStateException("TypeExcludeFilter " + getClass() + " has not implemented equals");
	}

	@Override
	public int hashCode() {
		throw new IllegalStateException("TypeExcludeFilter " + getClass() + " has not implemented hashCode");
	}

}
