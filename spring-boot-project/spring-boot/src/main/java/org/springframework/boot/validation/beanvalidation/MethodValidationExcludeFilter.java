/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.validation.beanvalidation;

import java.lang.annotation.Annotation;

import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;

/**
 * A filter for excluding types from method validation.
 * <p>用于排除方法验证中的类型的过滤器。
 *
 * @author Andy Wilkinson
 * @since 2.4.0
 * @see FilteredMethodValidationPostProcessor
 */
public interface MethodValidationExcludeFilter {

	/**
	 * Evaluate whether to exclude the given {@code type} from method validation.
	 * 评估是否将给定的{@code type}从方法验证中排除。
	 * @param type the type to evaluate
	 * 要评估的类型
	 * @return {@code true} to exclude the type from method validation, otherwise
	 * {@code false}.
	 * 如果要从方法验证中排除该类型，则返回{@code true}，否则返回{@code false}。
	 */
	boolean isExcluded(Class<?> type);

	/**
	 * Factory method to create a {@link MethodValidationExcludeFilter} that excludes
	 * classes by annotation found using an {@link SearchStrategy#INHERITED_ANNOTATIONS
	 * inherited annotations search strategy}.
	 * 工厂方法，创建一个使用{@link SearchStrategy#INHERITED_ANNOTATIONS 继承的注解搜索策略}
	 * 来排除类的{@link MethodValidationExcludeFilter}。
	 * @param annotationType the annotation to check
	 * 要检查的注解类型
	 * @return a {@link MethodValidationExcludeFilter} instance
	 * 一个{@link MethodValidationExcludeFilter}实例
	 */
	static MethodValidationExcludeFilter byAnnotation(Class<? extends Annotation> annotationType) {
		return byAnnotation(annotationType, SearchStrategy.INHERITED_ANNOTATIONS);
	}

	/**
	 * Factory method to create a {@link MethodValidationExcludeFilter} that excludes
	 * classes by annotation found using the given search strategy.
	 * 工厂方法，创建一个根据给定搜索策略通过注解排除类的{@link MethodValidationExcludeFilter}。
	 * @param annotationType the annotation to check
	 * 要检查的注解类型
	 * @param searchStrategy the annotation search strategy
	 * 注解搜索策略
	 * @return a {@link MethodValidationExcludeFilter} instance
	 */
	static MethodValidationExcludeFilter byAnnotation(Class<? extends Annotation> annotationType,
			SearchStrategy searchStrategy) {
		// 判断给定类的注解是否存在
		return (type) -> MergedAnnotations.from(type, searchStrategy).isPresent(annotationType);
	}

}
