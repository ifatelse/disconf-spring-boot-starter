package com.lethe.disconf.spring.property;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * @Author : Lethe
 * @Date : 2022/12/1 14:00
 * @Version : 1.0
 * @Copyright : Copyright (c) 2022 All Rights Reserved
 **/
public class SpringValueProcessor implements BeanPostProcessor, BeanFactoryAware {

    private static final Logger logger = LoggerFactory.getLogger(SpringValueProcessor.class);

    private final SpringValueRegistry springValueRegistry;

    private BeanFactory beanFactory;

    public SpringValueProcessor(SpringValueRegistry springValueRegistry) {
        this.springValueRegistry = springValueRegistry;
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
        ReflectionUtils.doWithFields(clazz, new ReflectionUtils.FieldCallback() {
            @Override
            public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
                res.add(field);
            }
        });
        return res;
    }

    private List<Method> findAllMethod(Class<?> clazz) {
        final List<Method> res = new LinkedList<>();
        ReflectionUtils.doWithMethods(clazz, new ReflectionUtils.MethodCallback() {
            @Override
            public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
                res.add(method);
            }
        });
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
            SpringValue springValue = new SpringValue(key, value.value(), bean, beanName, field, false);
            springValueRegistry.register(beanFactory, key, springValue);
            logger.debug("Monitoring {}", springValue);
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
            logger.warn("Ignore @Value setter {}.{}, expecting 1 parameter, actual {} parameters",
                    bean.getClass().getName(), method.getName(), method.getParameterTypes().length);
            return;
        }

        Set<String> keys = PlaceholderHelper.extractPlaceholderKeys(value.value());

        if (keys.isEmpty()) {
            return;
        }

        for (String key : keys) {
            SpringValue springValue = new SpringValue(key, value.value(), bean, beanName, method, false);
            springValueRegistry.register(beanFactory, key, springValue);
            logger.debug("Monitoring {}", springValue);
        }
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
}
