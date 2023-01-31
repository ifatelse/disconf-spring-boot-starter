package com.lethe.disconf.spring;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @Description :
 * @Author : liudd12
 * @Date : 2022/12/1 14:00
 * @Version : 1.0
 * @Copyright : Copyright (c) 2022 All Rights Reserved
 **/
public class AnnoValueProcessor implements BeanPostProcessor, BeanFactoryAware {

    private static final Log log = LogFactory.getLog(AnnoValueProcessor.class);

    private final AnnoValueRegistry annoValueRegistry;

    private BeanFactory beanFactory;

    public AnnoValueProcessor(AnnoValueRegistry annoValueRegistry) {
        this.annoValueRegistry = annoValueRegistry;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {

        Class<?> clazz = bean.getClass();
        for (Field field : findAllField(clazz)) {
            processField(bean, beanName, field);
        }
        for (Method method : findAllMethod(clazz)) {
            processMethod(bean, beanName, method);
        }

        return bean;
    }

    private List<Field> findAllField(Class<?> clazz) {
        final List<Field> res = new LinkedList<>();
        ReflectionUtils.doWithFields(clazz, res::add);
        return res;
    }

    private List<Method> findAllMethod(Class<?> clazz) {
        final List<Method> res = new LinkedList<>();
        ReflectionUtils.doWithMethods(clazz, res::add);
        return res;
    }

    protected void processField(Object bean, String beanName, Field field) {
        // register @Value on field
        Value value = field.getAnnotation(Value.class);
        if (value == null) {
            return;
        }
        Set<String> keys = PlaceholderHelper.extractPlaceholderKeys(value.value());

        if (keys.isEmpty()) {
            return;
        }

        for (String key : keys) {
            AnnoValue annoValue = new AnnoValue(key, value.value(), bean, beanName, field, false);
            annoValueRegistry.register(beanFactory, key, annoValue);
            log.debug("Monitoring " + annoValue);
        }
    }


    protected void processMethod(Object bean, String beanName, Method method) {
        // register @Value on method
        Value value = method.getAnnotation(Value.class);
        if (value == null) {
            return;
        }
        // skip Configuration bean methods
        if (method.getAnnotation(Bean.class) != null) {
            return;
        }
        if (method.getParameterTypes().length != 1) {
            log.warn("Ignore @Value setter " + bean.getClass().getName() + "." + method.getName() +
                    ", expecting 1 parameter, actual " + method.getParameterTypes().length + " parameters");
            return;
        }

        Set<String> keys = PlaceholderHelper.extractPlaceholderKeys(value.value());

        if (keys.isEmpty()) {
            return;
        }

        for (String key : keys) {
            AnnoValue annoValue = new AnnoValue(key, value.value(), bean, beanName, method, false);
            annoValueRegistry.register(beanFactory, key, annoValue);
            log.debug("Monitoring " + annoValue);
        }
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
}
