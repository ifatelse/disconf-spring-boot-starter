package com.lethe.disconf.internals;


import com.baidu.disconf.client.common.model.DisConfCommonModel;
import com.baidu.disconf.client.common.model.DisconfCenterFile;
import com.baidu.disconf.client.config.DisClientSysConfig;
import com.baidu.disconf.client.fetcher.FetcherMgr;
import com.baidu.disconf.core.common.constants.Constants;
import com.baidu.disconf.core.common.json.ValueVo;
import com.baidu.disconf.core.common.path.DisconfWebPathMgr;
import com.baidu.disconf.core.common.utils.DisconfThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @Description :
 * @Author : Lethe
 * @Date : 2022/12/6 13:38
 * @Version : 1.0
 * @Copyright : Copyright (c) 2022 All Rights Reserved
 **/
public class DisconfContextRefresher implements ApplicationContextAware, ApplicationListener<ContextRefreshedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(DisconfContextRefresher.class);

    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(), DisconfThreadFactory.create("RemoteConfigLongPollService", true));

    private ApplicationContext applicationContext;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        for (Map.Entry<String, RemoteConfigRepository> repositoryEntry : DisConfConfigService.getInstance().listenerMap().entrySet()) {
            executorService.execute(new LongPollingRunnable(repositoryEntry.getKey(), repositoryEntry.getValue()));
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    class LongPollingRunnable implements Runnable {

        private final String key;

        private final RemoteConfigRepository configRepository;

        public LongPollingRunnable(String key, RemoteConfigRepository configRepository) {
            this.key = key;
            this.configRepository = configRepository;
        }

        @Override
        public void run() {
            try {
                DisconfCenterFile disconfCenterFile = configRepository.disconfCenterFile;
                FetcherMgr fetcherMgr = configRepository.fetcherMgr;

                String url = assembleLongPollRefreshUrl(key, disconfCenterFile);

                ValueVo valueVo = fetcherMgr.getChangeFileFromServer(url);
                if (Objects.equals(valueVo.getStatus(), Constants.CONFIG_CHANGE)) {
                    logger.info("config change");
                    notifyChange(valueVo.getValue(), fetcherMgr, disconfCenterFile);
                }
                executorService.execute(this);
            } catch (Exception e) {
                logger.error("longPolling error : ", e);
                executorService.schedule(this, 5000, TimeUnit.MILLISECONDS);
            }
        }
    }

    private void notifyChange(String fileName, FetcherMgr fetcherMgr, DisconfCenterFile disconfCenterFile) {
        applicationContext.publishEvent(new ConfigChangeEvent("config_change", fetcherMgr, disconfCenterFile, fileName));
    }

    private String assembleLongPollRefreshUrl(String fileName, DisconfCenterFile disconfCenterFile) {
        DisConfCommonModel disConfCommonModel = disconfCenterFile.getDisConfCommonModel();
        String app = disConfCommonModel.getApp();
        String version = disConfCommonModel.getVersion();
        String env = disConfCommonModel.getEnv();
        return DisconfWebPathMgr.getRemoteUrlParameter(DisClientSysConfig.getInstance().CONF_SERVER_NOTIFY_ACTION, app, version, env, fileName);
        // return DisconfWebPathMgr.getRemoteUrlParameter("/api/notify/async", app, version, env, confLoads, DisConfigTypeEnum.FILE);
    }


}
