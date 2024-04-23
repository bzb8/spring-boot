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

package org.springframework.boot.context.annotation;

import java.util.Set;

import org.springframework.beans.factory.Aware;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Interface that can be implemented by {@link ImportSelector} and
 * {@link ImportBeanDefinitionRegistrar} implementations when they can determine imports
 * early. The {@link ImportSelector} and {@link ImportBeanDefinitionRegistrar} interfaces
 * are quite flexible which can make it hard to tell exactly what bean definitions they
 * will add. This interface should be used when an implementation consistently results in
 * the same imports, given the same source.
 * <p>
 * Using {@link DeterminableImports} is particularly useful when working with Spring's
 * testing support. It allows for better generation of {@link ApplicationContext} cache
 * keys.
 * <p>可以被{@link ImportSelector}和{@link ImportBeanDefinitionRegistrar}实现者实现的接口，
 * 当它们能够提前确定导入时使用。{@link ImportSelector}和{@link ImportBeanDefinitionRegistrar}接口非常灵活，
 * 这可能使得很难确切知道它们会添加哪些bean定义。当一个实现给定相同的源时始终产生相同的导入时，
 * 应该使用此接口。
 * <p>
 * 使用{@link DeterminableImports}在使用Spring的测试支持时特别有用。它允许更好地生成{@link ApplicationContext}缓存键。
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.5.0
 */
@FunctionalInterface
public interface DeterminableImports {

	/**
	 * Return a set of objects that represent the imports. Objects within the returned
	 * {@code Set} must implement a valid {@link Object#hashCode() hashCode} and
	 * {@link Object#equals(Object) equals}.
	 * <p>
	 * Imports from multiple {@link DeterminableImports} instances may be combined by the
	 * caller to create a complete set.
	 * <p>
	 * Unlike {@link ImportSelector} and {@link ImportBeanDefinitionRegistrar} any
	 * {@link Aware} callbacks will not be invoked before this method is called.
	 * <P>返回代表导入的一组对象。返回的{@code Set}内的对象必须实现有效的{@link Object#hashCode() hashCode}和
	 * {@link Object#equals(Object) equals}。
	 * <p>
	 * 可以将多个{@link DeterminableImports}实例的导入组合由调用者创建一个完整的集合。
	 * <p>
	 * 与{@link ImportSelector}和{@link ImportBeanDefinitionRegistrar}不同，任何{@link Aware}回调
	 * 在调用此方法之前不会被调用。
	 * @param metadata the source meta-data
	 * @return a key representing the annotations that actually drive the import
	 */
	Set<Object> determineImports(AnnotationMetadata metadata);

}
