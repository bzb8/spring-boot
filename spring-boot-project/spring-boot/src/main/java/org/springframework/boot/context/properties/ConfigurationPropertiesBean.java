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

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.validation.annotation.Validated;

/**
 * Provides access to {@link ConfigurationProperties @ConfigurationProperties} bean
 * details, regardless of if the annotation was used directly or on a {@link Bean @Bean}
 * factory method. This class can be used to access {@link #getAll(ApplicationContext)
 * all} configuration properties beans in an ApplicationContext, or
 * {@link #get(ApplicationContext, Object, String) individual beans} on a case-by-case
 * basis (for example, in a {@link BeanPostProcessor}).
 * ConfigurationPropertiesBean类提供了访问使用了{@link ConfigurationProperties @ConfigurationProperties}注解的bean的详情的方法，
 * 无论这些注解是直接使用的还是用在了{@link Bean @Bean}工厂方法上。这个类可以用于访问一个ApplicationContext中的{@link #getAll(ApplicationContext) 所有配置属性bean}，
 * 或者是按需访问{@link #get(ApplicationContext, Object, String) 单个bean}（例如，在一个{@link BeanPostProcessor}中使用）。
 *
 * <p>ConfigurationPropertiesBean封装了一个需要绑定的bean，跟bindable的区别是ConfigurationPropertiesBean跟spring里bean是对应的，
 * 而bindable指的是一个能被绑定的source，可以是类、instance、或者一个field，bindable可以脱离spring使用
 *
 * @author Phillip Webb
 * @since 2.2.0
 * @see #getAll(ApplicationContext)
 * @see #get(ApplicationContext, Object, String)
 */
public final class ConfigurationPropertiesBean {
	// beanName
	private final String name;

	// bean实例
	private final Object instance;

	private final ConfigurationProperties annotation;

	private final Bindable<?> bindTarget;
	// bindMethod=VALUE_OBJECT时instance为null
	private final BindMethod bindMethod;

	private ConfigurationPropertiesBean(String name, Object instance, ConfigurationProperties annotation,
			Bindable<?> bindTarget) {
		this.name = name;
		this.instance = instance;
		this.annotation = annotation;
		this.bindTarget = bindTarget;
		this.bindMethod = BindMethod.forType(bindTarget.getType().resolve());
	}

	/**
	 * Return the name of the Spring bean.
	 * @return the bean name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Return the actual Spring bean instance.
	 * @return the bean instance
	 */
	public Object getInstance() {
		return this.instance;
	}

	/**
	 * Return the bean type.
	 * @return the bean type
	 */
	Class<?> getType() {
		return this.bindTarget.getType().resolve();
	}

	/**
	 * Return the property binding method that was used for the bean.
	 * @return the bind type
	 */
	public BindMethod getBindMethod() {
		return this.bindMethod;
	}

	/**
	 * Return the {@link ConfigurationProperties} annotation for the bean. The annotation
	 * may be defined on the bean itself or from the factory method that create the bean
	 * (usually a {@link Bean @Bean} method).
	 * @return the configuration properties annotation
	 */
	public ConfigurationProperties getAnnotation() {
		return this.annotation;
	}

	/**
	 * Return a {@link Bindable} instance suitable that can be used as a target for the
	 * {@link Binder}.
	 * @return a bind target for use with the {@link Binder}
	 */
	public Bindable<?> asBindTarget() {
		return this.bindTarget;
	}

	/**
	 * Return all {@link ConfigurationProperties @ConfigurationProperties} beans contained
	 * in the given application context. Both directly annotated beans, as well as beans
	 * that have {@link ConfigurationProperties @ConfigurationProperties} annotated
	 * factory methods are included.
	 * @param applicationContext the source application context
	 * @return a map of all configuration properties beans keyed by the bean name
	 */
	public static Map<String, ConfigurationPropertiesBean> getAll(ApplicationContext applicationContext) {
		Assert.notNull(applicationContext, "ApplicationContext must not be null");
		if (applicationContext instanceof ConfigurableApplicationContext) {
			return getAll((ConfigurableApplicationContext) applicationContext);
		}
		Map<String, ConfigurationPropertiesBean> propertiesBeans = new LinkedHashMap<>();
		applicationContext.getBeansWithAnnotation(ConfigurationProperties.class).forEach((beanName, bean) -> {
			ConfigurationPropertiesBean propertiesBean = get(applicationContext, bean, beanName);
			if (propertiesBean != null) {
				propertiesBeans.put(beanName, propertiesBean);
			}
		});
		return propertiesBeans;
	}

	private static Map<String, ConfigurationPropertiesBean> getAll(ConfigurableApplicationContext applicationContext) {
		Map<String, ConfigurationPropertiesBean> propertiesBeans = new LinkedHashMap<>();
		ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();
		Iterator<String> beanNames = beanFactory.getBeanNamesIterator();
		while (beanNames.hasNext()) {
			String beanName = beanNames.next();
			if (isConfigurationPropertiesBean(beanFactory, beanName)) {
				try {
					Object bean = beanFactory.getBean(beanName);
					ConfigurationPropertiesBean propertiesBean = get(applicationContext, bean, beanName);
					if (propertiesBean != null) {
						propertiesBeans.put(beanName, propertiesBean);
					}
				}
				catch (Exception ex) {
				}
			}
		}
		return propertiesBeans;
	}

	private static boolean isConfigurationPropertiesBean(ConfigurableListableBeanFactory beanFactory, String beanName) {
		try {
			if (beanFactory.getBeanDefinition(beanName).isAbstract()) {
				return false;
			}
			if (beanFactory.findAnnotationOnBean(beanName, ConfigurationProperties.class) != null) {
				return true;
			}
			Method factoryMethod = findFactoryMethod(beanFactory, beanName);
			return findMergedAnnotation(factoryMethod, ConfigurationProperties.class).isPresent();
		}
		catch (NoSuchBeanDefinitionException ex) {
			return false;
		}
	}

	/**
	 * Return a {@link ConfigurationPropertiesBean @ConfigurationPropertiesBean} instance
	 * for the given bean details or {@code null} if the bean is not a
	 * {@link ConfigurationProperties @ConfigurationProperties} object. Annotations are
	 * considered both on the bean itself, as well as any factory method (for example a
	 * {@link Bean @Bean} method).
	 * <p>根据给定的bean详情，返回一个{@link ConfigurationPropertiesBean @ConfigurationPropertiesBean}实例。
	 * 如果bean不是{@link ConfigurationProperties @ConfigurationProperties}对象，则返回{@code null}。
	 * 注解既会在bean本身考虑，也会在任何工厂方法（例如{@link Bean @Bean}方法）上考虑。
	 *
	 * @param applicationContext the source application context
	 * 源应用上下文
	 * @param bean the bean to consider
	 * 要考虑的bean
	 * @param beanName the bean name
	 * @return a configuration properties bean or {@code null} if the neither the bean nor
	 * factory method are annotated with
	 * {@link ConfigurationProperties @ConfigurationProperties}
	 * 如果bean或工厂方法有被{@link ConfigurationProperties @ConfigurationProperties}注解标记，
	 * 则返回一个配置属性bean，否则返回{@code null}
	 *
	 */
	public static ConfigurationPropertiesBean get(ApplicationContext applicationContext, Object bean, String beanName) {
		// 查找工厂方法
		Method factoryMethod = findFactoryMethod(applicationContext, beanName);
		// 基于bean名称、bean实例、bean类以及工厂方法创建配置属性bean实例
		return create(beanName, bean, bean.getClass(), factoryMethod);
	}

	private static Method findFactoryMethod(ApplicationContext applicationContext, String beanName) {
		if (applicationContext instanceof ConfigurableApplicationContext) {
			return findFactoryMethod((ConfigurableApplicationContext) applicationContext, beanName);
		}
		return null;
	}

	private static Method findFactoryMethod(ConfigurableApplicationContext applicationContext, String beanName) {
		return findFactoryMethod(applicationContext.getBeanFactory(), beanName);
	}

	private static Method findFactoryMethod(ConfigurableListableBeanFactory beanFactory, String beanName) {
		if (beanFactory.containsBeanDefinition(beanName)) {
			BeanDefinition beanDefinition = beanFactory.getMergedBeanDefinition(beanName);
			if (beanDefinition instanceof RootBeanDefinition) {
				Method resolvedFactoryMethod = ((RootBeanDefinition) beanDefinition).getResolvedFactoryMethod();
				// 如果已解析的工厂方法非空，直接返回该方法
				if (resolvedFactoryMethod != null) {
					return resolvedFactoryMethod;
				}
			}
			// 如果在根bean定义中未找到已解析工厂方法，尝试使用反射从bean定义中查找
			return findFactoryMethodUsingReflection(beanFactory, beanDefinition);
		}
		return null;
	}

	private static Method findFactoryMethodUsingReflection(ConfigurableListableBeanFactory beanFactory,
			BeanDefinition beanDefinition) {
		String factoryMethodName = beanDefinition.getFactoryMethodName();
		String factoryBeanName = beanDefinition.getFactoryBeanName();
		// factoryBeanName为空 || factoryMethodName为空，直接返回null
		if (factoryMethodName == null || factoryBeanName == null) {
			return null;
		}
		Class<?> factoryType = beanFactory.getType(factoryBeanName);
		if (factoryType.getName().contains(ClassUtils.CGLIB_CLASS_SEPARATOR)) {
			factoryType = factoryType.getSuperclass();
		}
		AtomicReference<Method> factoryMethod = new AtomicReference<>();
		// 查找工厂类中等于factoryMethodName的方法
		ReflectionUtils.doWithMethods(factoryType, (method) -> {
			if (method.getName().equals(factoryMethodName)) {
				factoryMethod.set(method);
			}
		});
		return factoryMethod.get();
	}

	static ConfigurationPropertiesBean forValueObject(Class<?> beanClass, String beanName) {
		ConfigurationPropertiesBean propertiesBean = create(beanName, null, beanClass, null);
		Assert.state(propertiesBean != null && propertiesBean.getBindMethod() == BindMethod.VALUE_OBJECT,
				() -> "Bean '" + beanName + "' is not a @ConfigurationProperties value object");
		return propertiesBean;
	}

	/**
	 * 创建一个 ConfigurationPropertiesBean 实例。
	 * 该方法通过给定的参数构建一个配置属性 Bean，首先查找给定实例或其类型上的 @ConfigurationProperties 和 @Validated 注解，
	 * 然后根据这些注解和实例信息创建一个 ConfigurationPropertiesBean 实例。
	 * <p>类上需要标注有@ConfigurationProperties注解的类
	 *
	 * @param name 配置属性的名称。
	 * @param instance 配置属性的实例，可以为 null。bean实例
	 * @param type 配置属性的类型，当 instance 为 null 时特别有用。标注@ConfigurationProperties注解的类
	 * @param factory 用于创建配置属性实例的方法，可以为 null。当提供此方法时，将使用方法的返回类型作为绑定类型。
	 * @return 一个配置好的 ConfigurationPropertiesBean 实例，如果未找到 @ConfigurationProperties 注解则返回 null。
	 */
	private static ConfigurationPropertiesBean create(String name, Object instance, Class<?> type, Method factory) {
		// 查找 @ConfigurationProperties 注解
		ConfigurationProperties annotation = findAnnotation(instance, type, factory, ConfigurationProperties.class);
		if (annotation == null) {
			return null;
		}
		// 查找 @Validated 注解
		Validated validated = findAnnotation(instance, type, factory, Validated.class);
		// 准备注解数组，用于绑定时的元数据说明
		Annotation[] annotations = (validated != null) ? new Annotation[] { annotation, validated }
				: new Annotation[] { annotation };
		// 根据 factory 方法或类类型确定绑定的类型
		ResolvableType bindType = (factory != null) ? ResolvableType.forMethodReturnType(factory)
				: ResolvableType.forClass(type);
		// 创建一个带有注解和绑定类型的可绑定目标对象
		Bindable<Object> bindTarget = Bindable.of(bindType).withAnnotations(annotations);
		if (instance != null) {
			// 如果存在实例，则在绑定目标中设置现有值
			bindTarget = bindTarget.withExistingValue(instance);
		}
		// 创建并返回 ConfigurationPropertiesBean 实例
		return new ConfigurationPropertiesBean(name, instance, annotation, bindTarget);
	}

	/**
	 * 在给定对象、类型、方法上查找指定注解。
	 *
	 * @param instance 需要查找注解的对象实例。
	 * @param type 对象的类型。
	 * @param factory 方法对象，用于尝试从该方法上查找注解。
	 * @param annotationType 需要查找的注解类型。
	 * @return 找到的注解实例，如果未找到则返回null。
	 * @param <A> 注解的类型，需继承自Annotation。
	 */
	private static <A extends Annotation> A findAnnotation(Object instance, Class<?> type, Method factory,
			Class<A> annotationType) {
		// 初始化为未找到的注解
		MergedAnnotation<A> annotation = MergedAnnotation.missing();
		// 尝试从方法上查找注解
		if (factory != null) {
			annotation = findMergedAnnotation(factory, annotationType);
		}
		// 如果在方法上未找到，尝试从类型上查找注解
		if (!annotation.isPresent()) {
			annotation = findMergedAnnotation(type, annotationType);
		}
		// 如果在类型上仍未找到注解，且实例是AOP代理，则尝试从其目标类上查找注解
		if (!annotation.isPresent() && AopUtils.isAopProxy(instance)) {
			annotation = MergedAnnotations.from(AopUtils.getTargetClass(instance), SearchStrategy.TYPE_HIERARCHY)
					.get(annotationType);
		}
		// 如果找到了注解，则合成并返回，否则返回null
		return annotation.isPresent() ? annotation.synthesize() : null;
	}

	/**
	 * 查找给定元素上合并的注解。
	 *
	 * @param element 要在其上查找注解的注解元素，如类、方法或字段等。
	 * @param annotationType 要查找的注解类型。
	 * @param <A> 注解的类型，该类型需扩展自Annotation。
	 * @return 如果找到指定类型的注解，则返回合并的注解实例；如果未找到，则返回一个表示缺失的注解实例。
	 */
	private static <A extends Annotation> MergedAnnotation<A> findMergedAnnotation(AnnotatedElement element,
			Class<A> annotationType) {
		// 判断元素是否非空，非空则在元素及其类型层次上搜索指定的注解，为空则返回一个表示注解缺失的实例
		return (element != null) ? MergedAnnotations.from(element, SearchStrategy.TYPE_HIERARCHY).get(annotationType)
				: MergedAnnotation.missing();
	}

	/**
	 * The binding method that is used for the bean.
	 * 定义了与bean绑定方法的枚举。
	 */
	public enum BindMethod {

		/**
		 * Java Bean using getter/setter binding.
		 * 使用getter/setter方法进行绑定的Java Bean。
		 */
		JAVA_BEAN,

		/**
		 * Value object using constructor binding.
		 * 使用构造函数进行绑定的值对象。构造函数上有@ConstructorBinding注解。
		 */
		VALUE_OBJECT;

		/**
		 * 根据给定的类型确定绑定方法。
		 * @param type 待绑定的类型的Class对象。
		 * @return 绑定方法，如果是通过构造函数绑定则返回VALUE_OBJECT，否则返回JAVA_BEAN。
		 */
		static BindMethod forType(Class<?> type) {
			// 判断类型是否可以通过构造函数绑定，如果是则返回VALUE_OBJECT，否则返回JAVA_BEAN
			return (ConfigurationPropertiesBindConstructorProvider.INSTANCE.getBindConstructor(type, false) != null)
					? VALUE_OBJECT : JAVA_BEAN;
		}

	}

}
