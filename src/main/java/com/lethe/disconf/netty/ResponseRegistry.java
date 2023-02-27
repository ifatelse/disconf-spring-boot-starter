package com.lethe.disconf.netty;

import com.baidu.disconf.core.common.remote.Response;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.*;

/**
 * @Description :
 * @Author : Lethe
 * @Date : 2023/2/23 11:02
 * @Version : 1.0
 * @Copyright : Copyright (c) 2023 All Rights Reserved
 **/
public class ResponseRegistry {

    private static final Map<String, Class<?>> REGISTRY_RESPONSE = new HashMap<>();

    static boolean initialized = false;

    public static void init() {
        if (initialized) {
            return;
        }

        List<String> responseScanPackage = Collections.singletonList("com.baidu.disconf.core.common.remote");
        for (String pkg : responseScanPackage) {
            Reflections reflections = new Reflections(pkg);
            Set<Class<? extends Response>> subTypesOfResponse = reflections.getSubTypesOf(Response.class);
            for (Class<?> clazz : subTypesOfResponse) {

                if (Modifier.isAbstract(clazz.getModifiers())) {
                    return;
                }
                if (Modifier.isInterface(clazz.getModifiers())) {
                    return;
                }
                REGISTRY_RESPONSE.put(clazz.getSimpleName(), clazz);
            }
        }
        initialized = true;
    }

    public static Class<?> getClassByType(String type) {
        return REGISTRY_RESPONSE.get(type);
    }

}
