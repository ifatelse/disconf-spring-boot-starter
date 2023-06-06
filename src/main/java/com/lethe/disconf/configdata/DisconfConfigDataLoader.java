package com.lethe.disconf.configdata;

import com.baidu.disconf.client.common.model.DisConfCommonModel;
import com.baidu.disconf.client.common.model.DisconfCenterFile;
import com.baidu.disconf.client.config.ConfigMgr;
import com.baidu.disconf.client.config.DisClientConfig;
import com.baidu.disconf.client.watch.ClientInvokeDelegate;
import com.lethe.disconf.extension.ExtensionLoader;
import com.lethe.disconf.internals.ConfigRepositoryManager;
import com.lethe.disconf.utils.LoadFileUtils;
import com.lethe.disconf.utils.PropertySourceLoaderUtils;
import org.springframework.boot.context.config.ConfigData;
import org.springframework.boot.context.config.ConfigDataLoader;
import org.springframework.boot.context.config.ConfigDataLoaderContext;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import org.springframework.core.env.PropertySource;

import java.io.IOException;
import java.util.List;

/**
 * @Description :
 * @Author : Lethe
 * @Date : 2023/6/2 14:19
 * @Version : 1.0
 * @Copyright : Copyright (c) 2023 All Rights Reserved
 **/
public class DisconfConfigDataLoader implements ConfigDataLoader<DisconfConfigDataResource> {


    @Override
    public ConfigData load(ConfigDataLoaderContext context, DisconfConfigDataResource resource) throws IOException, ConfigDataResourceNotFoundException {

        String fileName = resource.getConfName();

        ConfigService.configLoad(fileName);

        if (LoadFileUtils.canLoadFileExtension(fileName)) {

            List<PropertySource<?>> propertySources = PropertySourceLoaderUtils.loadProperty(fileName);

            return new ConfigData(propertySources);
        }
        return null;
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

}
