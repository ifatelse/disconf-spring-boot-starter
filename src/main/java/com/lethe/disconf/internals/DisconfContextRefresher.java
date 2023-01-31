package com.lethe.disconf.internals;

import com.baidu.disconf.client.common.model.DisConfCommonModel;
import com.baidu.disconf.client.common.model.DisconfCenterFile;
import com.baidu.disconf.client.config.DisClientSysConfig;
import com.baidu.disconf.client.fetcher.FetcherMgr;
import com.baidu.disconf.core.common.constants.Constants;
import com.baidu.disconf.core.common.json.ValueVo;
import com.baidu.disconf.core.common.path.DisconfWebPathMgr;
import com.lethe.disconf.utils.DisconfThreadFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.cloud.endpoint.event.RefreshEvent;
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
 * @Author : liudd12
 * @Date : 2022/12/9 11:21
 * @Version : 1.0
 * @Copyright : Copyright (c) 2022 All Rights Reserved
 **/
public class DisconfContextRefresher implements ApplicationContextAware, ApplicationListener<ContextRefreshedEvent> {

    private static final Log log = LogFactory.getLog(DisconfContextRefresher.class);

    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(), DisconfThreadFactory.create("RemoteConfigLongPollService", true));


    private ApplicationContext applicationContext;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        for (Map.Entry<String, RemoteConfigRepository> listenerEntry : ConfigRepositoryManager.getInstance().listenerMap().entrySet()) {
            executorService.execute(new LongPollingRunnable(listenerEntry.getKey(), listenerEntry.getValue()));
        }
    }


    class LongPollingRunnable implements Runnable {

        private final String fileName;

        private final RemoteConfigRepository configRepository;

        public LongPollingRunnable(String fileName, RemoteConfigRepository configRepository) {
            this.fileName = fileName;
            this.configRepository = configRepository;
        }

        @Override
        public void run() {
            try {
                DisconfCenterFile disconfCenterFile = configRepository.disconfCenterFile;
                FetcherMgr fetcherMgr = configRepository.fetcherMgr;
                String url = assembleLongPollUrl(fileName, disconfCenterFile);

                ValueVo valueVo = fetcherMgr.getChangeFileFromServer(url);
                if (Objects.equals(valueVo.getStatus(), Constants.CONFIG_CHANGE)) {
                    log.info("config changed: " + fileName);
                    notifyChange(valueVo.getValue());
                }
                executorService.execute(this);

            } catch (Exception e) {
                log.error("longPolling error : ", e);
                executorService.schedule(this, 5000, TimeUnit.MILLISECONDS);
            }
        }

    }


    private void notifyChange(String fileName) {
        ConfigRepositoryManager.getInstance().confChange(fileName);
        applicationContext.publishEvent(new RefreshEvent(this, fileName, "Refresh Leconf Config"));
    }

    private String assembleLongPollUrl(String fileName, DisconfCenterFile disconfCenterFile) {
        DisConfCommonModel disConfCommonModel = disconfCenterFile.getDisConfCommonModel();
        String app = disConfCommonModel.getApp();
        String version = disConfCommonModel.getVersion();
        String env = disConfCommonModel.getEnv();
        return DisconfWebPathMgr.getRemoteUrlParameter(DisClientSysConfig.getInstance().CONF_SERVER_NOTIFY_ACTION, app, version, env, fileName);
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
