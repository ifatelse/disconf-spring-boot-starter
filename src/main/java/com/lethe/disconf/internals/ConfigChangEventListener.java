package com.lethe.disconf.internals;

import com.baidu.disconf.client.common.model.DisConfCommonModel;
import com.baidu.disconf.client.common.model.DisconfCenterFile;
import com.baidu.disconf.client.common.update.IDisconfUpdate;
import com.baidu.disconf.client.config.DisClientSysConfig;
import com.baidu.disconf.client.fetcher.FetcherMgr;
import com.baidu.disconf.core.common.constants.DisConfigTypeEnum;
import com.baidu.disconf.core.common.path.DisconfWebPathMgr;
import com.lethe.disconf.registry.DisconfUpdateRegistrar;
import com.lethe.disconf.scope.event.RefreshEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.*;

import java.util.List;

/**
 * @author : Lethe
 * @version : 1.0
 * @Description : 文件改变监听事件处理
 * @Date : 2020/11/15 11:41
 * @Copyright : Copyright (c) 2020 All Rights Reserved
 **/
public class ConfigChangEventListener implements ApplicationContextAware, ApplicationListener<ConfigChangeEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ConfigChangEventListener.class);

    private ApplicationContext applicationContext;

    @Override
    public void onApplicationEvent(ConfigChangeEvent event) {

        DisconfCenterFile disconfCenterFile = event.getDisconfCenterFile();
        FetcherMgr fetcherMgr = event.getFetcherMgr();
        String classPath = disconfCenterFile.getFileDir();
        String fileName = event.getFileName();
        DisConfCommonModel disConfCommonModel = disconfCenterFile.getDisConfCommonModel();
        String url = DisconfWebPathMgr.getRemoteUrlParameter(DisClientSysConfig.getInstance().CONF_SERVER_STORE_ACTION, disConfCommonModel.getApp(), disConfCommonModel.getVersion(), disConfCommonModel.getEnv(), fileName, DisConfigTypeEnum.FILE);

        logger.info("config change event:" + fileName);

        try {
            fetcherMgr.downloadFileFromServer(url, fileName, classPath);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // PropertySourceLoaderUtils.loadProperty(Collections.singletonList(DisconfAutoConfiguration.CONF_PREFIX + fileName), environment);

        applicationContext.publishEvent(
                new RefreshEvent(this, fileName, "Refresh Disconf config"));

        DisconfUpdateRegistrar disconfUpdateRegistrar = applicationContext.getBean(DisconfUpdateRegistrar.class);
        List<IDisconfUpdate> confUpdateList = disconfUpdateRegistrar.getConfLoadList(fileName);
        for (IDisconfUpdate disconfUpdate : confUpdateList) {
            try {
                disconfUpdate.reload();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
