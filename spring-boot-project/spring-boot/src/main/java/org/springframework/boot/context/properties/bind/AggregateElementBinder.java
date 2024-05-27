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

package org.springframework.boot.context.properties.bind;

import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;

/**
 * Binder that can be used by {@link AggregateBinder} implementations to recursively bind
 * elements.
 * 一个用于{@link AggregateBinder}实现的绑定器，可以递归地绑定元素。
 * 该类提供了方法来绑定各种类型的元素，以支持复杂依赖关系的注入和管理。
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
@FunctionalInterface
interface AggregateElementBinder {

	/**
	 * Bind the given name to a target bindable.
	 * @param name the name to bind
	 * @param target the target bindable
	 * @return a bound object or {@code null}
	 */
	default Object bind(ConfigurationPropertyName name, Bindable<?> target) {
		return bind(name, target, null);
	}

	/**
	 * Bind the given name to a target bindable using optionally limited to a single
	 * source.
	 * 将给定的名称绑定到一个目标可绑定对象上，可以选择限制为单个源。
	 * @param name the name to bind
	 * 要绑定的名称。
	 * @param target the target bindable
	 * 目标可绑定对象。
	 * @param source the source of the elements or {@code null} to use all sources
	 * 元素的源，或为 {@code null} 以使用所有源。
	 * @return a bound object or {@code null}
	 * 绑定的对象，或 {@code null}。
	 */
	Object bind(ConfigurationPropertyName name, Bindable<?> target, ConfigurationPropertySource source);

}
