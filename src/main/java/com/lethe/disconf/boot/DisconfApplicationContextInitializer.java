package com.lethe.disconf.boot;

import com.baidu.disconf.client.common.constants.SupportFileTypeEnum;
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
import com.lethe.disconf.internals.RemoteConfigRepository;
import com.lethe.disconf.netty.NettyChannelService;
import com.lethe.disconf.netty.NettyClient;
import com.lethe.disconf.utils.LoadFileUtils;
import com.lethe.disconf.utils.PropertySourceLoaderUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @Description : 加载配置文件内容
 * @Author : Lethe
 * @Date : 2022/11/22 17:01
 * @Version : 1.0
 * @Copyright : Copyright (c) 2022 All Rights Reserved
 **/
public class DisconfApplicationContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final Log log = LogFactory.getLog(DisconfApplicationContextInitializer.class);

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        try {
            ConfigurableEnvironment environment = applicationContext.getEnvironment();

            String confLoads = environment.getProperty(DisconfProperties.CONF_LOAD);
            if (!StringUtils.hasText(confLoads)) {
                return;
            }

            Set<String> confLoadSet = StringUtils.commaDelimitedListToSet(confLoads);

            ConfigMgr.init();

            NettyClient.start();

            if (!DisClientConfig.getInstance().ENABLE_DISCONF) {
                // String userDefineDownloadDir = DisClientConfig.getInstance().userDefineDownloadDir;
                // String localDownloadDir = LoadFileUtils.localDownloadDir(userDefineDownloadDir);
                Collection<String> canLoadFile = LoadFileUtils.loadFileExtension(confLoadSet);
                List<String> confLoadList = canLoadFile.stream().map(conf -> DisconfProperties.CONF_PREFIX + conf).collect(Collectors.toList());
                PropertySourceLoaderUtils.loadProperty(confLoadList, environment);
                log.info("load the local config success");
                return;
            }

            FetcherMgr fetcherMgr = FetcherFactory.getFetcherMgr();

            DisconfCenterFile disconfCenterFile = getDisconfCenterFile();

            for (String fileName : confLoadSet) {

                String url = assembleDownloadUrl(fileName, disconfCenterFile.getDisConfCommonModel());
                String classPath = disconfCenterFile.getFileDir();
                // fetcherMgr.downloadFileFromServer(url, fileName, classPath);

                NettyChannelService.loadDisconfData(fileName, disconfCenterFile.getDisConfCommonModel());

                if (LoadFileUtils.canLoadFileExtension(fileName)) {
                    PropertySourceLoaderUtils.loadProperty(Collections.singletonList(DisconfProperties.CONF_PREFIX + fileName), environment);
                }
            }

            ConfigRepositoryManager.getInstance().loadRemoteConfigRepository(new RemoteConfigRepository(fetcherMgr, disconfCenterFile));

            log.info("load the remote config success");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static DisconfCenterFile getLeconfCenterFile(String fileName) {
        DisconfCenterFile leconfCenterFile = new DisconfCenterFile();
        fileName = fileName.trim();
        leconfCenterFile.setFileName(fileName);
        leconfCenterFile.setSupportFileTypeEnum(SupportFileTypeEnum.getByFileName(fileName));
        DisConfCommonModel disConfCommonModel = makeDisConfCommonModel();
        leconfCenterFile.setDisConfCommonModel(disConfCommonModel);
        String url = DisconfWebPathMgr.getRemoteUrlParameter(DisClientSysConfig.getInstance().CONF_SERVER_STORE_ACTION, disConfCommonModel.getApp(), disConfCommonModel.getVersion(), disConfCommonModel.getEnv(), leconfCenterFile.getFileName(), DisConfigTypeEnum.FILE);
        leconfCenterFile.setRemoteServerUrl(url);
        return leconfCenterFile;
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

    private static String assembleDownloadUrl(String fileName, DisConfCommonModel disConfCommonModel) {
        String app = disConfCommonModel.getApp();
        String version = disConfCommonModel.getVersion();
        String env = disConfCommonModel.getEnv();
        return DisconfWebPathMgr.getRemoteUrlParameter(DisClientSysConfig.getInstance().CONF_SERVER_STORE_ACTION, app, version, env, fileName, DisConfigTypeEnum.FILE);
    }


}
