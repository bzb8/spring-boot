/*
 * Copyright 2012-2020 the original author or authors.
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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Enable support for {@link ConfigurationProperties @ConfigurationProperties} annotated
 * beans. {@code @ConfigurationProperties} beans can be registered in the standard way
 * (for example using {@link Bean @Bean} methods) or, for convenience, can be specified
 * directly on this annotation.
 * <p>启用{@link ConfigurationProperties @ConfigurationProperties}注解的bean的支持。
 * 通过此注解，可以以标准方式注册{@code @ConfigurationProperties} beans（例如使用{@link Bean @Bean}方法），
 * 或者为了方便起见，可以直接在该注解上指定。
 *
 * @author Dave Syer
 * @since 1.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(EnableConfigurationPropertiesRegistrar.class)
public @interface EnableConfigurationProperties {

	/**
	 * The bean name of the configuration properties validator.
	 * 配置属性验证器的bean名称。
	 * @since 2.2.0
	 */
	String VALIDATOR_BEAN_NAME = "configurationPropertiesValidator";

	/**
	 * Convenient way to quickly register
	 * {@link ConfigurationProperties @ConfigurationProperties} annotated beans with
	 * Spring. Standard Spring Beans will also be scanned regardless of this value.
	 * 方便地快速注册
	 * {@link ConfigurationProperties @ConfigurationProperties}注解的beans到Spring。
	 * 无论此值如何，标准Spring Beans也将被扫描。
	 *
	 * @return {@code @ConfigurationProperties} annotated beans to register
	 * 要注册的{@code @ConfigurationProperties}注解的beans类
	 */
	Class<?>[] value() default {};

}
