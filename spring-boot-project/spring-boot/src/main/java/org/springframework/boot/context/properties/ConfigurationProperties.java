/*
 * Copyright 2012-2021 the original author or authors.
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Indexed;

/**
 * Annotation for externalized configuration. Add this to a class definition or a
 * {@code @Bean} method in a {@code @Configuration} class if you want to bind and validate
 * some external Properties (e.g. from a .properties file).
 * <p>用于外部化配置的注解。如果想要绑定和验证一些外部属性（例如来自.properties文件的属性），
 * 可以将此注解添加到类定义或{@code @Configuration}类中的{@code @Bean}方法上。
 *
 * <p>
 * Binding is either performed by calling setters on the annotated class or, if
 * {@link ConstructorBinding @ConstructorBinding} is in use, by binding to the constructor
 * parameters.
 * <p>
 * 绑定可以通过调用注解类的setter方法来完成，或者如果使用了{@link ConstructorBinding @ConstructorBinding}，
 * 则可以通过绑定到构造函数参数来完成。
 *
 * <p>
 * Note that contrary to {@code @Value}, SpEL expressions are not evaluated since property
 * values are externalized.
 * <p>
 * 请注意，与{@code @Value}不同，由于属性值被外部化，所以不评估SpEL表达式。
 *
 * @author Dave Syer
 * @since 1.0.0
 * @see ConfigurationPropertiesScan
 * @see ConstructorBinding
 * @see ConfigurationPropertiesBindingPostProcessor
 * @see EnableConfigurationProperties
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Indexed
public @interface ConfigurationProperties {

	/**
	 * The prefix of the properties that are valid to bind to this object. Synonym for
	 * {@link #prefix()}. A valid prefix is defined by one or more words separated with
	 * dots (e.g. {@code "acme.system.feature"}).
	 * <p>属性的有效绑定前缀。是{@link #prefix()}的同义词。一个有效的前缀由点分隔的一个
	 * 或多个单词组成（例如{@code "acme.system.feature"}）。
	 * @return the prefix of the properties to bind
	 * 属性绑定的前缀
	 */
	@AliasFor("prefix")
	String value() default "";

	/**
	 * The prefix of the properties that are valid to bind to this object. Synonym for
	 * {@link #value()}. A valid prefix is defined by one or more words separated with
	 * dots (e.g. {@code "acme.system.feature"}).
	 * <p>是{@link #value()}的同义词
	 * @return the prefix of the properties to bind
	 */
	@AliasFor("value")
	String prefix() default "";

	/**
	 * Flag to indicate that when binding to this object invalid fields should be ignored.
	 * Invalid means invalid according to the binder that is used, and usually this means
	 * fields of the wrong type (or that cannot be coerced into the correct type).
	 * <p>标志，指示在将此对象绑定时，是否应该忽略无效的字段。无效字段是指根据使用的绑定器而言的，
	 * 通常这意味着类型错误（或无法强制转换为正确类型的字段）。
	 * @return the flag value (default false)
	 */
	boolean ignoreInvalidFields() default false;

	/**
	 * Flag to indicate that when binding to this object unknown fields should be ignored.
	 * An unknown field could be a sign of a mistake in the Properties.
	 * 标志，指示在将此对象绑定时，是否应该忽略未知的字段。未知字段可能是Properties中的错误的迹象。
	 * @return the flag value (default true)
	 */
	boolean ignoreUnknownFields() default true;

}
