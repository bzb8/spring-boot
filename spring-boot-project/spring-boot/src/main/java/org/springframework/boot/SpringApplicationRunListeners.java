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

package org.springframework.boot;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.logging.Log;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.core.metrics.StartupStep;
import org.springframework.util.ReflectionUtils;

/**
 * A collection of {@link SpringApplicationRunListener}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Chris Bono
 */
class SpringApplicationRunListeners {
	// LogFactory.getLog(SpringApplication.class);
	private final Log log;
	// spring-boot jar包中的META-INF/spring.factories中的监听器
	// org.springframework.boot.context.event.EventPublishingRunListener
	private final List<SpringApplicationRunListener> listeners;
	// DefaultApplicationStartup
	private final ApplicationStartup applicationStartup;

	SpringApplicationRunListeners(Log log, Collection<? extends SpringApplicationRunListener> listeners,
			ApplicationStartup applicationStartup) {
		this.log = log;
		this.listeners = new ArrayList<>(listeners);
		this.applicationStartup = applicationStartup;
	}

	/**
	 *
	 * @param bootstrapContext DefaultBootstrapContext
	 * @param mainApplicationClass
	 */
	void starting(ConfigurableBootstrapContext bootstrapContext, Class<?> mainApplicationClass) {
		// 1. 创建名为"spring.boot.application.starting"的DefaultStartupStep
		// 2. 依赖调用listeners的starting方法
		// 3. 对step的打tag
		doWithListeners("spring.boot.application.starting", (listener) -> listener.starting(bootstrapContext),
				(step) -> {
					if (mainApplicationClass != null) {
						step.tag("mainApplicationClass", mainApplicationClass.getName());
					}
				});
	}

	void environmentPrepared(ConfigurableBootstrapContext bootstrapContext, ConfigurableEnvironment environment) {
		doWithListeners("spring.boot.application.environment-prepared",
				(listener) -> listener.environmentPrepared(bootstrapContext, environment));
	}

	void contextPrepared(ConfigurableApplicationContext context) {
		doWithListeners("spring.boot.application.context-prepared", (listener) -> listener.contextPrepared(context));
	}

	void contextLoaded(ConfigurableApplicationContext context) {
		doWithListeners("spring.boot.application.context-loaded", (listener) -> listener.contextLoaded(context));
	}

	void started(ConfigurableApplicationContext context, Duration timeTaken) {
		doWithListeners("spring.boot.application.started", (listener) -> listener.started(context, timeTaken));
	}

	void ready(ConfigurableApplicationContext context, Duration timeTaken) {
		doWithListeners("spring.boot.application.ready", (listener) -> listener.ready(context, timeTaken));
	}

	void failed(ConfigurableApplicationContext context, Throwable exception) {
		doWithListeners("spring.boot.application.failed",
				(listener) -> callFailedListener(listener, context, exception), (step) -> {
					step.tag("exception", exception.getClass().toString());
					step.tag("message", exception.getMessage());
				});
	}

	private void callFailedListener(SpringApplicationRunListener listener, ConfigurableApplicationContext context,
			Throwable exception) {
		try {
			listener.failed(context, exception);
		}
		catch (Throwable ex) {
			if (exception == null) {
				ReflectionUtils.rethrowRuntimeException(ex);
			}
			if (this.log.isDebugEnabled()) {
				this.log.error("Error handling failed", ex);
			}
			else {
				String message = ex.getMessage();
				message = (message != null) ? message : "no error message";
				this.log.warn("Error handling failed (" + message + ")");
			}
		}
	}

	private void doWithListeners(String stepName, Consumer<SpringApplicationRunListener> listenerAction) {
		doWithListeners(stepName, listenerAction, null);
	}

	/**
	 * 对Spring应用程序启动过程中的监听器和步骤进行操作的私有方法。
	 *
	 * @param stepName 步骤的名称，用于标识启动过程中的具体步骤。
	 * @param listenerAction 一个消费者接口，接受SpringApplicationRunListener类型的参数，用于执行监听器的操作。
	 * @param stepAction 一个消费者接口，接受StartupStep类型的参数，用于执行启动步骤的操作。如果此参数为null，则不执行任何操作。
	 */
	private void doWithListeners(String stepName, Consumer<SpringApplicationRunListener> listenerAction,
			Consumer<StartupStep> stepAction) {

		// 启动一个名为stepName的启动步骤
		StartupStep step = this.applicationStartup.start(stepName);
		// 遍历并执行所有的监听器操作
		this.listeners.forEach(listenerAction);
		// 如果提供了stepAction，则执行此步骤操作
		if (stepAction != null) {
			stepAction.accept(step);
		}
		// 结束当前步骤
		step.end();
	}

}
