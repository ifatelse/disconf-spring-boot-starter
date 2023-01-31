package com.lethe.disconf.spring.boot;

import com.baidu.disconf.client.common.constants.SupportFileTypeEnum;
import com.baidu.disconf.client.common.model.DisConfCommonModel;
import com.baidu.disconf.client.common.model.DisconfCenterFile;
import com.baidu.disconf.client.config.ConfigMgr;
import com.baidu.disconf.client.config.DisClientConfig;
import com.baidu.disconf.client.config.DisClientSysConfig;
import com.baidu.disconf.client.fetcher.FetcherFactory;
import com.baidu.disconf.client.fetcher.FetcherMgr;
import com.lethe.disconf.internals.DisConfConfigService;
import com.lethe.disconf.internals.RemoteConfigRepository;
import com.lethe.disconf.utils.LoadFileUtils;
import com.lethe.disconf.utils.PropertySourceLoaderUtils;
import com.baidu.disconf.core.common.constants.DisConfigTypeEnum;
import com.baidu.disconf.core.common.path.DisconfWebPathMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @Description :
 * @Author : Lethe
 * @Date : 2022/11/11 19:50
 * @Version : 1.0
 * @Copyright : Copyright (c) 2022 All Rights Reserved
 **/
public class DisconfApplicationContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final Logger logger = LoggerFactory.getLogger(DisconfApplicationContextInitializer.class);

    private static final String XML_FILE_EXTENSION = ".xml";

    @Override
    public void initialize(@NonNull ConfigurableApplicationContext applicationContext) {

        try {

            ConfigurableEnvironment environment = applicationContext.getEnvironment();

            String confLoads = environment.getProperty(DisconfAutoConfiguration.CONF_LOAD);
            if (!StringUtils.hasText(confLoads)) {
                return;
            }

            Set<String> confLoadSet = StringUtils.commaDelimitedListToSet(confLoads);

            ConfigMgr.init();

            if (!DisClientConfig.getInstance().ENABLE_DISCONF) {
                String userDefineDownloadDir = DisClientConfig.getInstance().userDefineDownloadDir;
                String localDownloadDir = LoadFileUtils.localDownloadDir(userDefineDownloadDir);
                Collection<String> canLoadFile = LoadFileUtils.loadFileExtension(confLoadSet);
                List<String> confLoadList = canLoadFile.stream().map(conf -> localDownloadDir + conf).collect(Collectors.toList());
                PropertySourceLoaderUtils.loadProperty(confLoadList, environment);
                logger.info("load local config [" + LoadFileUtils.commaJoinToString(canLoadFile) + "], location: " + userDefineDownloadDir);
                return;
            }

            FetcherMgr fetcherMgr = FetcherFactory.getFetcherMgr();

            for (String fileName : confLoadSet) {

                if (!LoadFileUtils.canLoadFileExtension(fileName)) {
                    logger.warn("can't load " + fileName + ", support file extension is [" + LoadFileUtils.loadFileExtension() + "]");
                    continue;
                }

                DisconfCenterFile disconfCenterFile = getLeconfCenterFile(fileName);
                String url = disconfCenterFile.getRemoteServerUrl();
                String classPath = disconfCenterFile.getFileDir();
                fetcherMgr.downloadFileFromServer(url, fileName, classPath);

                DisConfConfigService.getInstance().addListener(fileName, new RemoteConfigRepository(fetcherMgr, disconfCenterFile));
                PropertySourceLoaderUtils.loadProperty(Collections.singletonList(DisconfAutoConfiguration.CONF_PREFIX + fileName), environment);

                logger.info("load remote config {} success", fileName);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }


    public static DisconfCenterFile getLeconfCenterFile(String fileName) {
        DisconfCenterFile disconfCenterFile = new DisconfCenterFile();
        fileName = fileName.trim();
        disconfCenterFile.setFileName(fileName);
        disconfCenterFile.setSupportFileTypeEnum(SupportFileTypeEnum.getByFileName(fileName));
        DisConfCommonModel disConfCommonModel = makeDisConfCommonModel();
        disconfCenterFile.setDisConfCommonModel(disConfCommonModel);
        String url = DisconfWebPathMgr.getRemoteUrlParameter(DisClientSysConfig.getInstance().CONF_SERVER_STORE_ACTION, disConfCommonModel.getApp(), disConfCommonModel.getVersion(), disConfCommonModel.getEnv(), disconfCenterFile.getFileName(), DisConfigTypeEnum.FILE);
        disconfCenterFile.setRemoteServerUrl(url);
        return disconfCenterFile;
    }

    protected static DisConfCommonModel makeDisConfCommonModel() {
        DisConfCommonModel disConfCommonModel = new DisConfCommonModel();
        disConfCommonModel.setApp(DisClientConfig.getInstance().APP);
        disConfCommonModel.setEnv(DisClientConfig.getInstance().ENV);
        disConfCommonModel.setVersion(DisClientConfig.getInstance().VERSION);
        return disConfCommonModel;
    }

}
