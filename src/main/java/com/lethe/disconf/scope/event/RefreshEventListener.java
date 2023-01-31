package com.lethe.disconf.scope.event;

import com.google.common.base.Objects;
import com.google.common.collect.Sets;
import com.lethe.disconf.spring.boot.DisconfAutoConfiguration;
import com.lethe.disconf.properties.EnvironmentChangeEvent;
import com.lethe.disconf.scope.refresh.RefreshScope;
import com.lethe.disconf.utils.PropertySourceLoaderUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;


public class RefreshEventListener implements ApplicationContextAware, EnvironmentAware, SmartApplicationListener {

    private static final Log log = LogFactory.getLog(RefreshEventListener.class);

    private ApplicationContext applicationContext;

    private ConfigurableEnvironment environment;

    private final RefreshScope scope;

    private final AtomicBoolean ready = new AtomicBoolean(false);

    public RefreshEventListener(RefreshScope scope) {
        this.scope = scope;
    }

    @Override
    public boolean supportsEventType(@NonNull Class<? extends ApplicationEvent> eventType) {
        return ApplicationReadyEvent.class.isAssignableFrom(eventType)
                || RefreshEvent.class.isAssignableFrom(eventType);
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationEvent event) {
        if (event instanceof ApplicationReadyEvent) {
            handle((ApplicationReadyEvent) event);
        } else if (event instanceof RefreshEvent) {
            handle((RefreshEvent) event);
        }
    }

    public void handle(ApplicationReadyEvent event) {
        this.ready.compareAndSet(false, true);
    }

    public void handle(RefreshEvent event) {
        if (this.ready.get()) {
            log.info("handle refresh-event");
            String fileName = event.getFileName();
            // 获取原文件值
            Map<String, Object> before = PropertySourceLoaderUtils.extractProperty(DisconfAutoConfiguration.CONF_PREFIX + fileName, environment);

            // 将改变的配置加载到环境变量中
            PropertySourceLoaderUtils.loadProperty(Collections.singletonList(DisconfAutoConfiguration.CONF_PREFIX + fileName), environment);

            Set<String> keys = calcChangeKeys(before, fileName);
            if (keys.isEmpty()) {
                return;
            }

            log.info("keys changed: " + keys);

            // @RefreshScope 注解 bean刷新
            this.scope.refreshAll();

            // @ConfigurationProperties 注解的类
            applicationContext.publishEvent(new EnvironmentChangeEvent(this.applicationContext, keys));
        }
    }

    private Set<String> calcChangeKeys(Map<String, Object> before, String fileName) {
        Map<String, Object> after = PropertySourceLoaderUtils.extractProperty(DisconfAutoConfiguration.CONF_PREFIX + fileName, environment);
        // 只比较相同key，新增和删除不做处理
        Set<String> commonKeys = Sets.intersection(before.keySet(), after.keySet());
        Set<String> keys = Sets.newHashSet();
        for (String commonKey : commonKeys) {
            if (Objects.equal(before.get(commonKey), after.get(commonKey))) {
                continue;
            }
            keys.add(commonKey);
        }
        return keys;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = (ConfigurableEnvironment) environment;
    }


}
