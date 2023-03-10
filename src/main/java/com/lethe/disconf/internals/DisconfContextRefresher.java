package com.lethe.disconf.internals;

import com.baidu.disconf.client.common.model.DisConfCommonModel;
import com.baidu.disconf.client.common.model.DisconfCenterFile;
import com.baidu.disconf.client.config.DisClientConfig;
import com.baidu.disconf.client.config.DisClientSysConfig;
import com.baidu.disconf.client.fetcher.FetcherMgr;
import com.baidu.disconf.client.watch.netty.NettyChannelExchanger;
import com.baidu.disconf.core.common.constants.Constants;
import com.baidu.disconf.core.common.constants.DisConfigTypeEnum;
import com.baidu.disconf.core.common.json.ValueVo;
import com.baidu.disconf.core.common.path.DisconfWebPathMgr;
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
        RemoteConfigRepository remoteConfigRepository = ConfigRepositoryManager.getInstance().getRemoteConfigRepository();
        if (remoteConfigRepository != null) {
            String type = applicationContext.getEnvironment().getProperty("listen.type", "http");
            log.info("listen config change type: " + type);

            if (Objects.equals(type, Constants.LISTEN_TYPE_HTTP)) {
                executorService.execute(new LongPollingRunnable(DisClientConfig.getInstance().APP, remoteConfigRepository));
            } else {
                NettyChannelExchanger nettyChannelExchanger = new NettyChannelExchanger("127.0.0.1", Constants.NETTY_PORT, new ConfigChangeResponseHandler(applicationContext));
                nettyChannelExchanger.executeConfigListen(DisClientConfig.getInstance().APP, remoteConfigRepository.disconfCenterFile.getDisConfCommonModel());
            }
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
                    String fileName = valueVo.getValue();
                    log.info("config changed: " + fileName);
                    notifyChange(fileName, configRepository);
                }
                executorService.execute(this);

            } catch (Exception e) {
                log.error("longPolling error : ", e);
                executorService.schedule(this, 5000, TimeUnit.MILLISECONDS);
            }
        }

    }


    private void notifyChange(String fileName, RemoteConfigRepository configRepository) {

        DisconfCenterFile disconfCenterFile = configRepository.disconfCenterFile;
        String classPath = disconfCenterFile.getFileDir();
        String url = assembleDownloadUrl(fileName, disconfCenterFile.getDisConfCommonModel());
        FetcherMgr fetcherMgr = configRepository.fetcherMgr;
        try {
            fetcherMgr.downloadFileFromServer(url, fileName, classPath);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (LoadFileUtils.canLoadFileExtension(fileName)) {
            ConfigRepositoryManager.getInstance().confChange(fileName);
            applicationContext.publishEvent(new RefreshEvent(this, fileName, "Refresh Leconf Config"));
        }

        applicationContext.publishEvent(new ConfigChangeEvent(this, fileName));

    }

    private String assembleLongPollUrl(String fileName, DisconfCenterFile disconfCenterFile) {
        DisConfCommonModel disConfCommonModel = disconfCenterFile.getDisConfCommonModel();
        String app = disConfCommonModel.getApp();
        String version = disConfCommonModel.getVersion();
        String env = disConfCommonModel.getEnv();
        return DisconfWebPathMgr.getRemoteUrlParameter(DisClientSysConfig.getInstance().CONF_SERVER_NOTIFY_ACTION, app, version, env, fileName);
    }

    private static String assembleDownloadUrl(String fileName, DisConfCommonModel disConfCommonModel) {
        String app = disConfCommonModel.getApp();
        String version = disConfCommonModel.getVersion();
        String env = disConfCommonModel.getEnv();
        return DisconfWebPathMgr.getRemoteUrlParameter(DisClientSysConfig.getInstance().CONF_SERVER_STORE_ACTION, app, version, env, fileName, DisConfigTypeEnum.FILE);
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
