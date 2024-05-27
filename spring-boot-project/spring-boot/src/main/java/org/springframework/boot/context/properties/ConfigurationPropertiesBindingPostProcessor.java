/*
 * Copyright 2012-2023 the original author or authors.
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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.context.properties.ConfigurationPropertiesBean.BindMethod;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.PropertySources;
import org.springframework.util.Assert;

/**
 * {@link BeanPostProcessor} to bind {@link PropertySources} to beans annotated with
 * {@link ConfigurationProperties @ConfigurationProperties}.
 * 该类是一个BeanPostProcessor的实现，用于将PropertySources绑定到使用了@ConfigurationProperties注解的bean上。
 * 它在Spring应用程序上下文中起到关键作用，负责将属性文件的值加载到对应的bean属性中。
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Christian Dupuis
 * @author Stephane Nicoll
 * @author Madhura Bhave
 * @since 1.0.0
 */
public class ConfigurationPropertiesBindingPostProcessor
		implements BeanPostProcessor, PriorityOrdered, ApplicationContextAware, InitializingBean {

	/**
	 * The bean name that this post-processor is registered with.
	 * 此后处理器注册时使用的 Bean 名称。
	 */
	public static final String BEAN_NAME = ConfigurationPropertiesBindingPostProcessor.class.getName();

	private ApplicationContext applicationContext;

	private BeanDefinitionRegistry registry;

	private ConfigurationPropertiesBinder binder;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// We can't use constructor injection of the application context because
		// it causes eager factory bean initialization
		// 此方法不使用构造器注入应用上下文，因为这会导致急切的工厂bean初始化。
		this.registry = (BeanDefinitionRegistry) this.applicationContext.getAutowireCapableBeanFactory();
		this.binder = ConfigurationPropertiesBinder.get(this.applicationContext);
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE + 1;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		// 绑定配置属性的值到标注了@ConfigurationProperties注解的bean
		bind(ConfigurationPropertiesBean.get(this.applicationContext, bean, beanName));
		return bean;
	}

	private void bind(ConfigurationPropertiesBean bean) {
		// 如果bean为空或者BindMethod属性的值为VALUE_OBJECT，则直接返回
		if (bean == null || hasBoundValueObject(bean.getName())) {
			return;
		}
		Assert.state(bean.getBindMethod() == BindMethod.JAVA_BEAN, "Cannot bind @ConfigurationProperties for bean '"
				+ bean.getName() + "'. Ensure that @ConstructorBinding has not been applied to regular bean");
		try {
			this.binder.bind(bean);
		}
		catch (Exception ex) {
			throw new ConfigurationPropertiesBindException(bean, ex);
		}
	}

	/**
	 * 判断该bean定义上BindMethod属性的值为VALUE_OBJECT
	 *
	 * @param beanName
	 * @return
	 */
	private boolean hasBoundValueObject(String beanName) {
		return this.registry.containsBeanDefinition(beanName) && BindMethod.VALUE_OBJECT
			.equals(this.registry.getBeanDefinition(beanName).getAttribute(BindMethod.class.getName()));
	}

	/**
	 * Register a {@link ConfigurationPropertiesBindingPostProcessor} bean if one is not
	 * already registered.
	 * <p>注册一个 {@link ConfigurationPropertiesBindingPostProcessor} Bean，如果该 Bean 还未注册的话。
	 * @param registry the bean definition registry
	 * @since 2.2.0
	 */
	public static void register(BeanDefinitionRegistry registry) {
		Assert.notNull(registry, "Registry must not be null");
		// 检查是否已存在ConfigurationPropertiesBindingPostProcessor.class.getName()的Bean定义，若不存在，则进行注册
		if (!registry.containsBeanDefinition(BEAN_NAME)) {
			// 注册 ConfigurationPropertiesBindingPostProcessor Bean
			BeanDefinition definition = BeanDefinitionBuilder
				.rootBeanDefinition(ConfigurationPropertiesBindingPostProcessor.class)
				.getBeanDefinition();
			// 设置Bean定义的角色为基础设施
			definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			registry.registerBeanDefinition(BEAN_NAME, definition);
		}
		// 注册ConfigurationProperties绑定
		ConfigurationPropertiesBinder.register(registry);
	}

}
