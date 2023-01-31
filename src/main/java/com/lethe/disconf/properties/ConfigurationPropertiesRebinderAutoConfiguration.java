package com.lethe.disconf.properties;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindingPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

/**
 * Auto-configuration for {@link ConfigurationPropertiesRebinder}.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(ConfigurationPropertiesBindingPostProcessor.class)
public class ConfigurationPropertiesRebinderAutoConfiguration
        implements ApplicationContextAware, SmartInitializingSingleton {

    private ApplicationContext context;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) {
        this.context = applicationContext;
    }

    @Bean
    @ConditionalOnMissingBean(search = SearchStrategy.CURRENT)
    public static ConfigurationPropertiesBeans configurationPropertiesBeans() {
        return new ConfigurationPropertiesBeans();
    }

    @Bean
    @ConditionalOnMissingBean(search = SearchStrategy.CURRENT)
    public ConfigurationPropertiesRebinder configurationPropertiesRebinder(ConfigurationPropertiesBeans beans) {
        ConfigurationPropertiesRebinder rebinder = new ConfigurationPropertiesRebinder(beans);
        return rebinder;
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (this.context.getParent() != null) {
            ConfigurationPropertiesRebinder rebinder = this.context.getBean(ConfigurationPropertiesRebinder.class);
            for (String name : this.context.getParent().getBeanDefinitionNames()) {
                rebinder.rebind(name);
            }
        }
    }

}
