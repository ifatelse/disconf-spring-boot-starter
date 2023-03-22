package com.lethe.disconf.internals;

import com.baidu.disconf.client.watch.ClientInvokeDelegate;
import com.baidu.disconf.client.watch.ConfigChangeListener;
import com.lethe.disconf.extension.ExtensionLoader;
import com.lethe.disconf.utils.DisconfThreadFactory;
import com.lethe.disconf.utils.LoadFileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.cloud.endpoint.event.RefreshEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @Description :
 * @Author : liudd12
 * @Date : 2022/12/9 11:21
 * @Version : 1.0
 * @Copyright : Copyright (c) 2022 All Rights Reserved
 **/
public class DisconfContextRefresher implements ApplicationContextAware, ApplicationListener<ContextRefreshedEvent>, ConfigChangeListener {

    private static final Log log = LogFactory.getLog(DisconfContextRefresher.class);

    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(), DisconfThreadFactory.create("RemoteConfigLongPollService", true));

    private ApplicationContext applicationContext;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {

        String type = applicationContext.getEnvironment().getProperty("client.type", "http");
        ExtensionLoader<ClientInvokeDelegate> extensionLoader = ExtensionLoader.getExtensionLoader(ClientInvokeDelegate.class);
        ClientInvokeDelegate adaptiveExtension = extensionLoader.getAdaptiveExtension(type);

        adaptiveExtension.configListen(this);

    }

    @Override
    public void configChange(String fileName) {

        log.info("config changed: " + fileName);

        if (LoadFileUtils.canLoadFileExtension(fileName)) {
            ConfigRepositoryManager.getInstance().confChange(fileName);
            applicationContext.publishEvent(new RefreshEvent(this, fileName, "Refresh Leconf Config"));
        }

        applicationContext.publishEvent(new ConfigChangeEvent(this, fileName));

    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
