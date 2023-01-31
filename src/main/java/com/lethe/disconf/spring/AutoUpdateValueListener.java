package com.lethe.disconf.spring;

import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collection;

/**
 * @Description :
 * @Author : liudd12
 * @Date : 2022/12/01 17:46
 * @Version : 1.0
 * @Copyright : Copyright (c) 2022 All Rights Reserved
 **/
public class AutoUpdateValueListener implements ApplicationContextAware, ApplicationListener<EnvironmentChangeEvent> {

    private static final Log log = LogFactory.getLog(AutoUpdateValueListener.class);

    private final AnnoValueRegistry annoValueRegistry;

    private ConfigurableListableBeanFactory beanFactory;

    private TypeConverter typeConverter;

    private final boolean convertIfNecessaryWithFieldParameter;

    private final Gson gson;

    public AutoUpdateValueListener(AnnoValueRegistry annoValueRegistry) {
        this.annoValueRegistry = annoValueRegistry;
        this.convertIfNecessaryWithFieldParameter = typeConverterHasConvertIfNecessaryWithFieldParameter();
        this.gson = new Gson();
    }

    @Override
    public void onApplicationEvent(EnvironmentChangeEvent event) {
        for (String key : event.getKeys()) {
            Collection<AnnoValue> annoValues = annoValueRegistry.get(beanFactory, key);
            if (annoValues == null || annoValues.isEmpty()) {
                continue;
            }
            for (AnnoValue val : annoValues) {
                updateSpringValue(val);
            }
        }
    }

    private void updateSpringValue(AnnoValue annoValue) {
        try {
            // 解析对应的配置信息
            Object value = resolvePropertyValue(annoValue);
            annoValue.update(value);

            log.debug("Auto update leconf changed value successfully, new value: " + value + ", " + annoValue);
        } catch (Throwable ex) {
            log.error("Auto update leconf changed value failed, " + annoValue.toString(), ex);
        }
    }

    private Object resolvePropertyValue(AnnoValue annoValue) {

        Object value = PlaceholderHelper
                .resolvePropertyValue(beanFactory, annoValue.getBeanName(), annoValue.getPlaceholder());

        if (annoValue.isJson()) {
            value = parseJsonValue((String) value, annoValue.getGenericType());
        } else {
            if (annoValue.isField()) {
                // org.springframework.beans.TypeConverter#convertIfNecessary(java.lang.Object, java.lang.Class, java.lang.reflect.Field) is available from Spring 3.2.0+
                if (convertIfNecessaryWithFieldParameter) {
                    value = this.typeConverter.convertIfNecessary(value, annoValue.getTargetType(), annoValue.getField());
                } else {
                    value = this.typeConverter.convertIfNecessary(value, annoValue.getTargetType());
                }
            } else {
                value = this.typeConverter.convertIfNecessary(value, annoValue.getTargetType(), annoValue.getMethodParameter());
            }
        }

        return value;
    }

    private Object parseJsonValue(String json, Type targetType) {
        try {
            return gson.fromJson(json, targetType);
        } catch (Throwable ex) {
            log.error("Parsing json '" + json + "' to type " + targetType + " failed!", ex);
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
