package com.lethe.disconf.registry;

import com.baidu.disconf.client.common.annotations.DisconfUpdateService;
import com.baidu.disconf.client.common.update.IDisconfUpdate;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Map;

/**
 * @Description :
 * @Author : Lethe
 * @Date : 2022/11/18 11:03
 * @Version : 1.0
 * @Copyright : Copyright (c) 2022 All Rights Reserved
 **/
public class DisconfUpdateRegistrar implements SmartInitializingSingleton, ApplicationContextAware {


    private ApplicationContext applicationContext;

    private final Multimap<String, IDisconfUpdate> iDisconfUpdateMultimap =
            Multimaps.synchronizedSetMultimap(HashMultimap.create());

    @Override
    public void afterSingletonsInstantiated() {
        Map<String, IDisconfUpdate> beansOfType = applicationContext.getBeansOfType(IDisconfUpdate.class);
        for (IDisconfUpdate disconfUpdate : beansOfType.values()) {
            DisconfUpdateService annotation = AnnotationUtils.findAnnotation(disconfUpdate.getClass(), DisconfUpdateService.class);
            if (annotation != null) {
                String[] confFileKeys = annotation.confFileKeys();
                for (String confFileKey : confFileKeys) {
                    iDisconfUpdateMultimap.put(confFileKey, disconfUpdate);
                }
            }
        }
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }


    public List<IDisconfUpdate> getConfLoadList(String conf) {
        return Lists.newArrayList(iDisconfUpdateMultimap.get(conf));
    }

}