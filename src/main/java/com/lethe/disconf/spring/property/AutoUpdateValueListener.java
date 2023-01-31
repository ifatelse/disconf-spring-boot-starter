package com.lethe.disconf.spring.property;

import com.google.gson.Gson;
import com.lethe.disconf.properties.EnvironmentChangeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collection;

/**
 * @Description :
 * @Author : Lethe
 * @Date : 2022/12/1 17:46
 * @Version : 1.0
 * @Copyright : Copyright (c) 2022 All Rights Reserved
 **/
public class AutoUpdateValueListener implements ApplicationContextAware, ApplicationListener<EnvironmentChangeEvent> {

    private static final Logger logger = LoggerFactory.getLogger(AutoUpdateValueListener.class);

    private final SpringValueRegistry springValueRegistry;

    private ConfigurableListableBeanFactory beanFactory;

    private TypeConverter typeConverter;

    private final boolean convertIfNecessaryWithFieldParameter;

    private final Gson gson;

    public AutoUpdateValueListener(SpringValueRegistry springValueRegistry) {
        this.springValueRegistry = springValueRegistry;
        this.convertIfNecessaryWithFieldParameter = typeConverterHasConvertIfNecessaryWithFieldParameter();
        this.gson = new Gson();
    }

    @Override
    public void onApplicationEvent(EnvironmentChangeEvent event) {
        for (String key : event.getKeys()) {
            Collection<SpringValue> springValues = springValueRegistry.get(beanFactory, key);
            if (springValues == null || springValues.isEmpty()) {
                continue;
            }
            for (SpringValue val : springValues) {
                updateSpringValue(val);
            }
        }
    }

    private void updateSpringValue(SpringValue springValue) {
        try {
            // 解析对应的配置信息
            Object value = resolvePropertyValue(springValue);
            springValue.update(value);

            logger.info("Auto update leconf changed value successfully, new value: {}, {}", value, springValue);
        } catch (Throwable ex) {
            logger.error("Auto update leconf changed value failed, {}", springValue.toString(), ex);
        }
    }

    private Object resolvePropertyValue(SpringValue springValue) {

        Object value = PlaceholderHelper
                .resolvePropertyValue(beanFactory, springValue.getBeanName(), springValue.getPlaceholder());

        if (springValue.isJson()) {
            value = parseJsonValue((String) value, springValue.getGenericType());
        } else {
            if (springValue.isField()) {
                // org.springframework.beans.TypeConverter#convertIfNecessary(java.lang.Object, java.lang.Class, java.lang.reflect.Field) is available from Spring 3.2.0+
                if (convertIfNecessaryWithFieldParameter) {
                    value = this.typeConverter.convertIfNecessary(value, springValue.getTargetType(), springValue.getField());
                } else {
                    value = this.typeConverter.convertIfNecessary(value, springValue.getTargetType());
                }
            } else {
                value = this.typeConverter.convertIfNecessary(value, springValue.getTargetType(), springValue.getMethodParameter());
            }
        }

        return value;
    }

    private Object parseJsonValue(String json, Type targetType) {
        try {
            return gson.fromJson(json, targetType);
        } catch (Throwable ex) {
            logger.error("Parsing json '{}' to type {} failed!", json, targetType, ex);
            throw ex;
        }
    }

    private boolean typeConverterHasConvertIfNecessaryWithFieldParameter() {
        try {
            TypeConverter.class.getMethod("convertIfNecessary", Object.class, Class.class, Field.class);
        } catch (Throwable ex) {
            return false;
        }

        return true;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.beanFactory = ((ConfigurableApplicationContext) applicationContext).getBeanFactory();
        this.typeConverter = this.beanFactory.getTypeConverter();
    }
}
