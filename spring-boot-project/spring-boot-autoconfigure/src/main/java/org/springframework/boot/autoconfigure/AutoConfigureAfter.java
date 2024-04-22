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

package org.springframework.boot.autoconfigure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

/**
 * Hint for that an {@link EnableAutoConfiguration auto-configuration} should be applied
 * after other specified auto-configuration classes.
 * <p>
 * As with standard {@link Configuration @Configuration} classes, the order in which
 * auto-configuration classes are applied only affects the order in which their beans are
 * defined. The order in which those beans are subsequently created is unaffected and is
 * determined by each bean's dependencies and any {@link DependsOn @DependsOn}
 * relationships.
 * 为{@link EnableAutoConfiguration 自动配置}提供应用顺序的提示。
 * 该注解表明一个自动配置类应该在其他指定的自动配置类之后应用。
 * <p>
 * 和标准的{@link Configuration @Configuration}类一样，自动配置类的应用顺序只影响其定义的bean的顺序。
 * 而这些bean随后创建的顺序则不受影响，它们的创建顺序由每个bean的依赖关系以及任何{@link DependsOn @DependsOn}关系决定。
 *
 * @author Phillip Webb
 * @since 1.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
@Documented
public @interface AutoConfigureAfter {

	/**
	 * The auto-configure classes that should have already been applied.
	 * @return the classes
	 */
	Class<?>[] value() default {};

	/**
	 * The names of the auto-configure classes that should have already been applied.
	 * @return the class names
	 * @since 1.2.2
	 */
	String[] name() default {};

}
