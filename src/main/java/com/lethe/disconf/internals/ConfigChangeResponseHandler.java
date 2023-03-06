package com.lethe.disconf.internals;

import com.baidu.disconf.client.common.model.DisConfCommonModel;
import com.baidu.disconf.client.common.model.DisconfCenterFile;
import com.baidu.disconf.client.config.DisClientSysConfig;
import com.baidu.disconf.client.fetcher.FetcherMgr;
import com.baidu.disconf.core.common.constants.Constants;
import com.baidu.disconf.core.common.constants.DisConfigTypeEnum;
import com.baidu.disconf.core.common.path.DisconfWebPathMgr;
import com.baidu.disconf.core.common.remote.ConfigChangeResponse;
import com.baidu.disconf.core.common.remote.ResponseHandler;
import com.lethe.disconf.utils.LoadFileUtils;
import io.netty.channel.Channel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.endpoint.event.RefreshEvent;
import org.springframework.context.ApplicationContext;

import java.util.Objects;

/**
 * @Description :
 * @Author : Lethe
 * @Date : 2023/3/3 18:24
 * @Version : 1.0
 * @Copyright : Copyright (c) 2023 All Rights Reserved
 **/
public class ConfigChangeResponseHandler extends ResponseHandler<ConfigChangeResponse> {

    private static final Log log = LogFactory.getLog(ConfigChangeResponseHandler.class);

    private ApplicationContext applicationContext;

    public ConfigChangeResponseHandler(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void handle(ConfigChangeResponse response, Channel channel) {

        if (Objects.equals(response.getStatus(), Constants.CONFIG_CHANGE)) {
            String fileName = response.getFileName();
            log.info("config changed: " + fileName);
            notifyChange(fileName);
        }

    }

    private void notifyChange(String fileName) {
        RemoteConfigRepository remoteConfigRepository = ConfigRepositoryManager.getInstance().getRemoteConfigRepository();
        DisconfCenterFile disconfCenterFile = remoteConfigRepository.disconfCenterFile;
        String classPath = disconfCenterFile.getFileDir();
        String url = assembleDownloadUrl(fileName, disconfCenterFile.getDisConfCommonModel());
        FetcherMgr fetcherMgr = remoteConfigRepository.fetcherMgr;
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

    private static String assembleDownloadUrl(String fileName, DisConfCommonModel disConfCommonModel) {
        String app = disConfCommonModel.getApp();
        String version = disConfCommonModel.getVersion();
        String env = disConfCommonModel.getEnv();
        return DisconfWebPathMgr.getRemoteUrlParameter(DisClientSysConfig.getInstance().CONF_SERVER_STORE_ACTION, app, version, env, fileName, DisConfigTypeEnum.FILE);
    }


}
