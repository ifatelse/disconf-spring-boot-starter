package com.lethe.disconf.utils;

import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;

/**
 * @Description : 配置解析至环境变量中
 * @Author : liudd12
 * @Date : 2022/8/8 17:00
 * @Version : 1.0
 * @Copyright : Copyright (c) 2022 All Rights Reserved
 **/
public class PropertySourceLoaderUtils {

    private static final ResourceLoader resourceLoader = new DefaultResourceLoader();

    private static final List<PropertySourceLoader> propertySourceLoaders = SpringFactoriesLoader.loadFactories(PropertySourceLoader.class, PropertySourceLoaderUtils.class.getClassLoader());

    public static void loadProperty(List<String> confList, ConfigurableEnvironment environment) {
        for (String conf : confList) {
            for (PropertySourceLoader propertySourceLoader : propertySourceLoaders) {
                if (canLoadFileExtension(propertySourceLoader, conf)) {
                    Resource resource = resourceLoader.getResource(conf);
                    if (!resource.exists()) {
                        throw new IllegalArgumentException(conf + " does not exist");
                    }
                    try {
                        List<PropertySource<?>> propertySourceList = propertySourceLoader.load(conf, resource);
                        for (PropertySource<?> propertySource : propertySourceList) {
                            environment.getPropertySources().addLast(propertySource);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    public static List<PropertySource<?>> loadProperty(String conf) {
        try {
            for (PropertySourceLoader propertySourceLoader : propertySourceLoaders) {
                if (canLoadFileExtension(propertySourceLoader, conf)) {
                    Resource resource = resourceLoader.getResource(conf);
                    if (!resource.exists()) {
                        throw new IllegalArgumentException(conf + " does not exist");
                    }
                    List<PropertySource<?>> propertySources = propertySourceLoader.load(conf, resource);
                    if (CollectionUtils.isEmpty(propertySources)) {
                        propertySources = Collections.emptyList();
                    }
                    return propertySources;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return Collections.emptyList();
    }

    public static Map<String, Object> extractProperty(String conf, ConfigurableEnvironment environment) {
        Map<String, Object> result = new HashMap<>();
        PropertySource<?> propertySource = environment.getPropertySources().get(conf);
        extract(propertySource, result);
        return result;
    }

    private static void extract(PropertySource<?> propertySource, Map<String, Object> result) {
        if (propertySource instanceof CompositePropertySource) {
            try {
                List<PropertySource<?>> sources = new ArrayList<>();
                for (PropertySource<?> source : ((CompositePropertySource) propertySource).getPropertySources()) {
                    sources.add(0, source);
                }
                for (PropertySource<?> source : sources) {
                    extract(source, result);
                }
            } catch (Exception e) {
                // ignore
            }
        } else if (propertySource instanceof EnumerablePropertySource) {
            for (String key : ((EnumerablePropertySource<?>) propertySource).getPropertyNames()) {
                result.put(key, propertySource.getProperty(key));
            }
        }
    }

    private static boolean canLoadFileExtension(PropertySourceLoader loader, String name) {
        return Arrays.stream(loader.getFileExtensions())
                .anyMatch((fileExtension) -> StringUtils.endsWithIgnoreCase(name, fileExtension));
    }

}
