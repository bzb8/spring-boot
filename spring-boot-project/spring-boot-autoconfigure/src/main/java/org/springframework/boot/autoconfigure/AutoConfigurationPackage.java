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

package org.springframework.boot.autoconfigure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

/**
 * Registers packages with {@link AutoConfigurationPackages}. When no {@link #basePackages
 * base packages} or {@link #basePackageClasses base package classes} are specified, the
 * package of the annotated class is registered.
 * <p>注册包到{@link AutoConfigurationPackages}。当没有指定{@link #basePackages 基础包}
 * 或{@link #basePackageClasses 基础包类}时，将注解类所在的包进行注册。
 *
 * @author Phillip Webb
 * @since 1.3.0
 * @see AutoConfigurationPackages
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(AutoConfigurationPackages.Registrar.class)
public @interface AutoConfigurationPackage {

	/**
	 * Base packages that should be registered with {@link AutoConfigurationPackages}.
	 * <p>
	 * Use {@link #basePackageClasses} for a type-safe alternative to String-based package
	 * names.
	 * <p>声明应与{@link AutoConfigurationPackages}注册的基本包。
	 * <p>
	 * 如需以类型安全的方式指定包名，可使用{@link #basePackageClasses}。
	 * @return the back package names
	 * 返回基础包名的字符串数组
	 * @since 2.3.0
	 */
	String[] basePackages() default {};

	/**
	 * Type-safe alternative to {@link #basePackages} for specifying the packages to be
	 * registered with {@link AutoConfigurationPackages}.
	 * <p>
	 * Consider creating a special no-op marker class or interface in each package that
	 * serves no purpose other than being referenced by this attribute.
	 * 为指定与{@link AutoConfigurationPackages}注册的包提供类型安全的替代方案。
	 * <p>
	 * 考虑在每个包中创建一个无操作的标记类或接口，其唯一目的就是被此属性引用。
	 * @return the base package classes
	 * 返回基础包类的类数组
	 * @since 2.3.0
	 */
	Class<?>[] basePackageClasses() default {};

}
