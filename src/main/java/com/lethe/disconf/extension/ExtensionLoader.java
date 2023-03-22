package com.lethe.disconf.extension;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

/**
 * @Description :
 * @Author : Lethe
 * @Date : 2023/3/16 16:12
 * @Version : 1.0
 * @Copyright : Copyright (c) 2023 All Rights Reserved
 **/
public class ExtensionLoader<T> {

    private static final Logger logger = LoggerFactory.getLogger(ExtensionLoader.class);

    private static final Pattern NAME_SEPARATOR = Pattern.compile("\\s*[,]+\\s*");

    private static final ConcurrentMap<Class<?>, ExtensionLoader<?>> EXTENSION_LOADERS = new ConcurrentHashMap<>(64);

    private final Map<String, Class<?>> extensionClasses = new HashMap<>();

    private final Map<String, T> extensionInstance = new HashMap<>();

    private final Class<?> classType;

    public ExtensionLoader(Class<?> classType) {
        this.classType = classType;
    }

    @SuppressWarnings("unchecked")
    public static <T> ExtensionLoader<T> getExtensionLoader(Class<T> classType) {
        if (classType == null) {
            throw new IllegalArgumentException("Extension type == null");
        }
        if (!classType.isInterface()) {
            throw new IllegalArgumentException("Extension type (" + classType + ") is not an interface!");
        }
        ExtensionLoader<T> loader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(classType);
        if (loader == null) {
            // new ExtensionLoader 意味着每个接口都有个独立的ExtensionLoader
            EXTENSION_LOADERS.putIfAbsent(classType, new ExtensionLoader<T>(classType));
            loader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(classType);
        }
        return loader;
    }


    public T getAdaptiveExtension(String type) {
        try {
            T t = extensionInstance.get(type);
            if (t == null) {
                if (extensionClasses.isEmpty()) {
                    loadExtensionClasses();
                }
                t = (T) extensionClasses.get(type).newInstance();
                extensionInstance.put(type, t);
            }
            return t;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private void loadExtensionClasses() {

        String fileName = directory() + classType.getName();

        ClassLoader classLoader = findClassLoader();
        try {
            Enumeration<URL> urls = classLoader.getResources(fileName);
            if (urls != null) {
                while (urls.hasMoreElements()) {
                    java.net.URL resourceURL = urls.nextElement();
                    // 加载资源
                    loadResource(classLoader, resourceURL);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void loadResource(ClassLoader classLoader, URL resourceURL) {
        try {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceURL.openStream(), StandardCharsets.UTF_8))) {
                String line;
                // 按行读取配置内容
                while ((line = reader.readLine()) != null) {
                    // 定位 # 字符
                    final int ci = line.indexOf('#');
                    // 截取 # 之前的字符串，# 之后的内容为注释，需要忽略
                    if (ci >= 0) {
                        line = line.substring(0, ci);
                    }
                    line = line.trim();
                    if (line.length() > 0) {
                        try {
                            String name = null;
                            // 以等于号 = 为界，截取键与值
                            int i = line.indexOf('=');
                            if (i > 0) {
                                name = line.substring(0, i).trim();
                                line = line.substring(i + 1).trim();
                            }
                            if (line.length() > 0) {
                                // 加载类，并通过 loadClass 方法对类进行缓存
                                loadClass(Class.forName(line, true, classLoader), name);
                            }
                        } catch (Throwable t) {
                            throw new IllegalStateException("Failed to load extension class (interface: " + classType + ", class line: " + line + ") in " + resourceURL + ", cause: " + t.getMessage(), t);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            logger.error("Exception occurred when loading extension class (interface: " +
                    classType + ", class file: " + resourceURL + ") in " + resourceURL, t);
        }
    }

    private void loadClass(Class<?> clazz, String name) {
        String[] names = NAME_SEPARATOR.split(name);
        if (ArrayUtils.isNotEmpty(names)) {
            for (String n : names) {
                // extensionClasses 中建立name和clazz的映射
                saveInExtensionClass(clazz, n);
            }
        }
    }


    private void saveInExtensionClass(Class<?> clazz, String name) {
        try {
            Class<?> c = extensionClasses.get(name);
            if (c == null) {
                extensionClasses.put(name, clazz);
            } else if (c != clazz) {
                String duplicateMsg = "Duplicate extension " + classType.getName() + " name " + name + " on " + clazz.getName() + " and " + clazz.getName();
                logger.error(duplicateMsg);
                throw new IllegalStateException(duplicateMsg);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static ClassLoader findClassLoader() {
        return getClassLoader(ExtensionLoader.class);
    }

    public static ClassLoader getClassLoader(Class<?> clazz) {
        ClassLoader cl = null;
        try {
            cl = Thread.currentThread().getContextClassLoader();
        } catch (Throwable ex) {
            // Cannot access thread context ClassLoader - falling back to system class loader...
        }
        if (cl == null) {
            // No thread context class loader -> use class loader of this class.
            cl = clazz.getClassLoader();
            if (cl == null) {
                // getClassLoader() returning null indicates the bootstrap ClassLoader
                try {
                    cl = ClassLoader.getSystemClassLoader();
                } catch (Throwable ex) {
                    // Cannot access system ClassLoader - oh well, maybe the caller can live with null...
                }
            }
        }

        return cl;
    }


    public String directory() {
        return "META-INF/services/";
    }

}
