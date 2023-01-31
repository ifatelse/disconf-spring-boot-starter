package com.lethe.disconf.properties;

import com.lethe.disconf.scope.refresh.RefreshScope;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.context.properties.ConfigurationPropertiesBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class ConfigurationPropertiesBeans implements BeanPostProcessor, ApplicationContextAware {

	private final Map<String, ConfigurationPropertiesBean> beans = new HashMap<>();

	private ApplicationContext applicationContext;

	private ConfigurableListableBeanFactory beanFactory;

	private String refreshScope;

	private boolean refreshScopeInitialized;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
		if (applicationContext.getAutowireCapableBeanFactory() instanceof ConfigurableListableBeanFactory) {
			this.beanFactory = (ConfigurableListableBeanFactory) applicationContext
					.getAutowireCapableBeanFactory();
		}
		if (applicationContext.getParent() != null && applicationContext.getParent()
				.getAutowireCapableBeanFactory() instanceof ConfigurableListableBeanFactory) {
			ConfigurableListableBeanFactory listable = (ConfigurableListableBeanFactory) applicationContext
					.getParent().getAutowireCapableBeanFactory();
			String[] names = listable.getBeanNamesForType(ConfigurationPropertiesBeans.class);
			if (names.length == 1) {
				ConfigurationPropertiesBeans parent = (ConfigurationPropertiesBeans) listable.getBean(names[0]);
				this.beans.putAll(parent.beans);
			}
		}
	}


	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (isRefreshScoped(beanName)) {
			return bean;
		}
		ConfigurationPropertiesBean propertiesBean = ConfigurationPropertiesBean
				.get(this.applicationContext, bean, beanName);
		if (propertiesBean != null) {
			//  收集类上含有 @ConfigurationProperties 注解的
			this.beans.put(beanName, propertiesBean);
		}
		return bean;
	}

	private boolean isRefreshScoped(String beanName) {
		if (this.refreshScope == null && !this.refreshScopeInitialized) {
			this.refreshScopeInitialized = true;
			for (String scope : this.beanFactory.getRegisteredScopeNames()) {
				if (this.beanFactory.getRegisteredScope(scope) instanceof RefreshScope) {
					this.refreshScope = scope;
					break;
				}
			}
		}
		if (beanName == null || this.refreshScope == null) {
			return false;
		}
		return this.beanFactory.containsBeanDefinition(beanName) && this.refreshScope
				.equals(this.beanFactory.getBeanDefinition(beanName).getScope());
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {
		return bean;
	}

	public Set<String> getBeanNames() {
		return new HashSet<String>(this.beans.keySet());
	}

}
