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

import org.springframework.boot.context.properties.bind.Binder.Context;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;

/**
 * Internal strategy used by {@link Binder} to bind data objects. A data object is an
 * object composed itself of recursively bound properties.
 * DataObjectBinder是一个接口，解决java对象绑定的问题。接口定义如下，有两个方法：bind和create
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @see JavaBeanBinder
 * @see ValueObjectBinder
 */
interface DataObjectBinder {

	/**
	 * Return a bound instance or {@code null} if the {@link DataObjectBinder} does not
	 * support the specified {@link Bindable}.
	 * 尝试将指定的 {@link Bindable} 对象绑定到一个实例上。如果 {@link DataObjectBinder} 不支持绑定给定的 {@link Bindable}，
	 * 则会返回 {@code null}。
	 *
	 * @param name the name being bound
	 * 被绑定的名称。
	 * @param target the bindable to bind
	 * 需要绑定的 {@link Bindable} 对象。
	 * @param context the bind context
	 * 绑定上下文，提供额外的绑定环境信息。
	 * @param propertyBinder property binder
	 * 属性绑定器，用于处理数据对象的属性绑定。
	 * @param <T> the source type
	 * 源类型的泛型参数。
	 * @return a bound instance or {@code null}
	 * 绑定后的实例，如果不支持绑定则返回 {@code null}。
	 */
	<T> T bind(ConfigurationPropertyName name, Bindable<T> target, Context context,
			DataObjectPropertyBinder propertyBinder);

	/**
	 * Return a newly created instance or {@code null} if the {@link DataObjectBinder}
	 * does not support the specified {@link Bindable}.
	 * @param target the bindable to create
	 * @param context the bind context
	 * @param <T> the source type
	 * @return the created instance
	 */
	<T> T create(Bindable<T> target, Context context);

}
