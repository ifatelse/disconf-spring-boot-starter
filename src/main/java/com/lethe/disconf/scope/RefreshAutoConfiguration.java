package com.lethe.disconf.scope;

import com.lethe.disconf.scope.event.RefreshEventListener;
import com.lethe.disconf.scope.refresh.RefreshScope;
import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Set;


@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(RefreshScope.class)
public class RefreshAutoConfiguration {

    /**
     * Name of the refresh scope name.
     */
    public static final String REFRESH_SCOPE_NAME = "refresh";

    @Bean
    @ConditionalOnMissingBean(RefreshScope.class)
    public static RefreshScope refreshScope() {
        return new RefreshScope();
    }


    @Bean
    public RefreshEventListener refreshEventListener(RefreshScope scope) {
        return new RefreshEventListener(scope);
    }


    @Component
    protected static class RefreshScopeBeanDefinitionEnhancer
            implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {

        private Environment environment;

        private boolean bound = false;

        /**
         * Class names for beans to post process into refresh scope. Useful when you don't
         * control the bean definition (e.g. it came from auto-configuration).
         */
        private Set<String> refreshables = new HashSet<>();

        public Set<String> getRefreshable() {
            return this.refreshables;
        }

        public void setRefreshable(Set<String> refreshables) {
            if (this.refreshables != refreshables) {
                this.refreshables.clear();
                this.refreshables.addAll(refreshables);
            }
        }

        public void setExtraRefreshable(Set<String> refreshables) {
            this.refreshables.addAll(refreshables);
        }

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

        }

        @Override
        public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
            bindEnvironmentIfNeeded(registry);
            for (String name : registry.getBeanDefinitionNames()) {
                BeanDefinition definition = registry.getBeanDefinition(name);
                if (isApplicable(registry, name, definition)) {
                    BeanDefinitionHolder holder = new BeanDefinitionHolder(definition, name);
                    BeanDefinitionHolder proxy = ScopedProxyUtils
                            .createScopedProxy(holder, registry, true);
                    definition.setScope("refresh");
                    if (registry.containsBeanDefinition(proxy.getBeanName())) {
                        registry.removeBeanDefinition(proxy.getBeanName());
                    }
                    registry.registerBeanDefinition(proxy.getBeanName(), proxy.getBeanDefinition());
                }
            }
        }

        private boolean isApplicable(BeanDefinitionRegistry registry, String name, BeanDefinition definition) {
            String scope = definition.getScope();
            if (REFRESH_SCOPE_NAME.equals(scope)) {
                // Already refresh scoped
                return false;
            }
            String type = definition.getBeanClassName();
            if (!StringUtils.hasText(type) && registry instanceof BeanFactory) {
                Class<?> cls = ((BeanFactory) registry).getType(name);
                if (cls != null) {
                    type = cls.getName();
                }
            }
            if (type != null) {
                return this.refreshables.contains(type);
            }
            return false;
        }

        private void bindEnvironmentIfNeeded(BeanDefinitionRegistry registry) {
            if (!this.bound) { // only bind once
                if (this.environment == null) {
                    this.environment = new StandardEnvironment();
                }
                Binder.get(this.environment).bind("spring.cloud.refresh",
                        Bindable.ofInstance(this));
                this.bound = true;
            }
        }

        @Override
        public void setEnvironment(Environment environment) {
            this.environment = environment;
        }

    }

}
