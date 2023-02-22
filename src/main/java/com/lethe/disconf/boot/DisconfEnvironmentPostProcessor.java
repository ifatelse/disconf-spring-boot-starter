package com.lethe.disconf.boot;

import com.lethe.disconf.internals.ConfigRepositoryManager;
import com.lethe.disconf.utils.PropertySourceLoaderUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Collections;
import java.util.Queue;

/**
 * @author : liudd12
 * @version : 1.0
 * @Description : 文件改变监听事件处理
 * @Date : 2020/11/15 11:41
 * @Copyright : Copyright (c) 2020 All Rights Reserved
 **/
public class DisconfEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Log log = LogFactory.getLog(DisconfEnvironmentPostProcessor.class);


    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {

        Queue<String> confQueue = ConfigRepositoryManager.getInstance().getconfChangeQueue();
        while (!confQueue.isEmpty()) {
            String fileName = confQueue.poll();
            PropertySourceLoaderUtils.loadProperty(Collections.singletonList(DisconfProperties.CONF_PREFIX + fileName), environment);

        }
    }
}
