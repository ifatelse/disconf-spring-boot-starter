package com.lethe.disconf.client;

import com.baidu.disconf.client.common.model.DisConfCommonModel;
import com.baidu.disconf.client.common.model.DisconfCenterFile;
import com.baidu.disconf.client.config.DisClientSysConfig;
import com.baidu.disconf.client.fetcher.FetcherFactory;
import com.baidu.disconf.client.fetcher.FetcherMgr;
import com.baidu.disconf.client.watch.ClientInvokeDelegate;
import com.baidu.disconf.client.watch.ConfigChangeListener;
import com.baidu.disconf.core.common.constants.Constants;
import com.baidu.disconf.core.common.constants.DisConfigTypeEnum;
import com.baidu.disconf.core.common.json.ValueVo;
import com.baidu.disconf.core.common.path.DisconfWebPathMgr;
import com.lethe.disconf.internals.ConfigRepositoryManager;
import com.lethe.disconf.utils.DisconfThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @Description :
 * @Author : Lethe
 * @Date : 2023/3/16 16:02
 * @Version : 1.0
 * @Copyright : Copyright (c) 2023 All Rights Reserved
 **/
public class HttpClientInvoke implements ClientInvokeDelegate {

    private static final Logger logger = LoggerFactory.getLogger(HttpClientInvoke.class);

    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(), DisconfThreadFactory.create("RemoteConfigLongPollService", true));

    private final FetcherMgr fetcherMgr;

    public HttpClientInvoke() {
        try {
            this.fetcherMgr = FetcherFactory.getFetcherMgr();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void configLoad(String fileName) {
        DisconfCenterFile disconfCenterFile = ConfigRepositoryManager.getInstance().getDisconfCenterFile();
        String url = assembleDownloadUrl(fileName, disconfCenterFile.getDisConfCommonModel());
        String classPath = disconfCenterFile.getFileDir();
        try {
            fetcherMgr.downloadFileFromServer(url, fileName, classPath);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void configListen(ConfigChangeListener configChangeListener) {
        DisconfCenterFile disconfCenterFile = ConfigRepositoryManager.getInstance().getDisconfCenterFile();
        executorService.execute(new ListenerConfigRunnable(disconfCenterFile, configChangeListener));
    }

    class ListenerConfigRunnable implements Runnable {

        private final DisconfCenterFile disconfCenterFile;

        private final ConfigChangeListener configChangeListener;

        public ListenerConfigRunnable(DisconfCenterFile disconfCenterFile, ConfigChangeListener configChangeListener) {
            this.disconfCenterFile = disconfCenterFile;
            this.configChangeListener = configChangeListener;
        }

        @Override
        public void run() {
            try {
                String longPollUrl = assembleLongPollUrl(disconfCenterFile);
                ValueVo valueVo = fetcherMgr.getChangeFileFromServer(longPollUrl);
                if (Objects.equals(valueVo.getStatus(), Constants.CONFIG_CHANGE)) {
                    String fileName = valueVo.getValue();
                    String classPath = disconfCenterFile.getFileDir();
                    // 下载文件
                    String downloadUrl = assembleDownloadUrl(fileName, disconfCenterFile.getDisConfCommonModel());
                    fetcherMgr.downloadFileFromServer(downloadUrl, fileName, classPath);
                    configChangeListener.configChange(fileName);
                }
                executorService.execute(this);

            } catch (Exception e) {
                logger.error("longPolling error : ", e);
                executorService.schedule(this, 5000, TimeUnit.MILLISECONDS);
            }
        }
    }


    private static String assembleDownloadUrl(String fileName, DisConfCommonModel disConfCommonModel) {
        String app = disConfCommonModel.getApp();
        String version = disConfCommonModel.getVersion();
        String env = disConfCommonModel.getEnv();
        return DisconfWebPathMgr.getRemoteUrlParameter(DisClientSysConfig.getInstance().CONF_SERVER_STORE_ACTION, app, version, env, fileName, DisConfigTypeEnum.FILE);
    }

    private String assembleLongPollUrl(DisconfCenterFile disconfCenterFile) {
        DisConfCommonModel disConfCommonModel = disconfCenterFile.getDisConfCommonModel();
        String app = disConfCommonModel.getApp();
        String version = disConfCommonModel.getVersion();
        String env = disConfCommonModel.getEnv();
        return DisconfWebPathMgr.getRemoteUrlParameter(DisClientSysConfig.getInstance().CONF_SERVER_NOTIFY_ACTION, app, version, env, app);
    }

}
