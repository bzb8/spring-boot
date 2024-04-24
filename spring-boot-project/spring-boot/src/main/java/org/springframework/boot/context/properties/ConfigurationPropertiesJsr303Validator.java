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

import org.springframework.boot.validation.MessageInterpolatorFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ClassUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * Validator that supports configuration classes annotated with
 * {@link Validated @Validated}.
 *
 * @author Phillip Webb
 */
final class ConfigurationPropertiesJsr303Validator implements Validator {

	private static final String[] VALIDATOR_CLASSES = { "javax.validation.Validator",
			"javax.validation.ValidatorFactory", "javax.validation.bootstrap.GenericBootstrap" };

	private final Delegate delegate;

	ConfigurationPropertiesJsr303Validator(ApplicationContext applicationContext) {
		this.delegate = new Delegate(applicationContext);
	}

	@Override
	public boolean supports(Class<?> type) {
		return this.delegate.supports(type);
	}

	@Override
	public void validate(Object target, Errors errors) {
		this.delegate.validate(target, errors);
	}

	/**
	 * 检查JSR 303验证器是否可用。
	 *
	 * @param applicationContext 应用上下文，用于获取类加载器。
	 * @return boolean 如果所有的验证器类都存在，则返回true；否则返回false。
	 */
	static boolean isJsr303Present(ApplicationContext applicationContext) {
		// 获取应用上下文的类加载器
		ClassLoader classLoader = applicationContext.getClassLoader();
		// 遍历所有验证器类
		for (String validatorClass : VALIDATOR_CLASSES) {
			// 如果任何一个验证器类不存在，则返回false
			if (!ClassUtils.isPresent(validatorClass, classLoader)) {
				return false;
			}
		}
		// 所有验证器类都存在，返回true
		return true;
	}

	private static class Delegate extends LocalValidatorFactoryBean {

		Delegate(ApplicationContext applicationContext) {
			setApplicationContext(applicationContext);
			setMessageInterpolator(new MessageInterpolatorFactory(applicationContext).getObject());
			afterPropertiesSet();
		}

	}

}
