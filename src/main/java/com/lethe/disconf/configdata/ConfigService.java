package com.lethe.disconf.configdata;

import com.baidu.disconf.client.common.model.DisConfCommonModel;
import com.baidu.disconf.client.common.model.DisconfCenterFile;
import com.baidu.disconf.client.config.ConfigMgr;
import com.baidu.disconf.client.config.DisClientConfig;
import com.baidu.disconf.client.config.DisClientSysConfig;
import com.baidu.disconf.client.fetcher.FetcherFactory;
import com.baidu.disconf.client.fetcher.FetcherMgr;
import com.baidu.disconf.core.common.constants.DisConfigTypeEnum;
import com.baidu.disconf.core.common.path.DisconfWebPathMgr;
import com.lethe.disconf.internals.ConfigRepositoryManager;

/**
 * @Description :
 * @Author : Lethe
 * @Date : 2023/6/6 15:35
 * @Version : 1.0
 * @Copyright : Copyright (c) 2023 All Rights Reserved
 **/
public class ConfigService {

    private static final ConfigService s_instance = new ConfigService();

    private volatile FetcherMgr fetcherMgr;

    public ConfigService() {
        try {
            ConfigMgr.init();
            DisconfCenterFile disconfCenterFile = getDisconfCenterFile();
            ConfigRepositoryManager.getInstance().loadDisconfCenterFile(disconfCenterFile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private FetcherMgr getFetcherMgr() throws Exception {
        if(fetcherMgr == null) {
            synchronized (this) {
                if(fetcherMgr == null) {
                    fetcherMgr = FetcherFactory.getFetcherMgr();
                }
            }
        }
        return fetcherMgr;
    }

    public static void configLoad(String fileName) {
        DisconfCenterFile disconfCenterFile = ConfigRepositoryManager.getInstance().getDisconfCenterFile();
        String url = assembleDownloadUrl(fileName);
        String classPath = disconfCenterFile.getFileDir();
        try {
            s_instance.getFetcherMgr().downloadFileFromServer(url, fileName, classPath);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }



    public static DisconfCenterFile getDisconfCenterFile() {
        DisconfCenterFile disconfCenterFile = new DisconfCenterFile();
        DisConfCommonModel disConfCommonModel = makeDisConfCommonModel();
        disconfCenterFile.setDisConfCommonModel(disConfCommonModel);
        return disconfCenterFile;
    }


    protected static DisConfCommonModel makeDisConfCommonModel() {
        DisConfCommonModel disConfCommonModel = new DisConfCommonModel();
        disConfCommonModel.setApp(DisClientConfig.getInstance().APP);
        disConfCommonModel.setEnv(DisClientConfig.getInstance().ENV);
        disConfCommonModel.setVersion(DisClientConfig.getInstance().VERSION);
        return disConfCommonModel;
    }

    private static String assembleDownloadUrl(String fileName) {
        String app = DisClientConfig.getInstance().APP;
        String version = DisClientConfig.getInstance().VERSION;
        String env = DisClientConfig.getInstance().ENV;
        return DisconfWebPathMgr.getRemoteUrlParameter(DisClientSysConfig.getInstance().CONF_SERVER_STORE_ACTION, app, version, env, fileName, DisConfigTypeEnum.FILE);
    }

}
