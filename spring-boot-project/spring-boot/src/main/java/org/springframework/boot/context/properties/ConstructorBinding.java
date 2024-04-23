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

/**
 * Annotation that can be used to indicate that configuration properties should be bound
 * using constructor arguments rather than by calling setters. Can be added at the type
 * level (if there is an unambiguous constructor) or on the actual constructor to use.
 * <p>这是一个Java注解@ConstructorBinding，其功能是指示应使用构造函数参数而非setter方法来绑定配置属性。
 * 该注解既可以应用于类型级别（若存在唯一无歧义的构造函数），也可以直接标注于所选用的构造函数之上。
 *
 * <p>
 * Note: To use constructor binding the class must be enabled using
 * {@link EnableConfigurationProperties @EnableConfigurationProperties} or configuration
 * property scanning. Constructor binding cannot be used with beans that are created by
 * the regular Spring mechanisms (e.g.
 * {@link org.springframework.stereotype.Component @Component} beans, beans created via
 * {@link org.springframework.context.annotation.Bean @Bean} methods or beans loaded using
 * {@link org.springframework.context.annotation.Import @Import}).
 * <p>
 * 使用提示：
 * 若要采用构造函数绑定，必须通过@EnableConfigurationProperties注解或进行配置属性扫描来启用相关类。
 * 构造函数绑定不可与通过常规Spring机制构建的bean（如：标记了@Component的bean、通过@Bean方法定义的bean，以及经由@Import引入的bean）共同使用。
 *
 * @author Phillip Webb
 * @since 2.2.0
 * @see ConfigurationProperties
 */
@Target({ ElementType.TYPE, ElementType.CONSTRUCTOR })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConstructorBinding {

}
