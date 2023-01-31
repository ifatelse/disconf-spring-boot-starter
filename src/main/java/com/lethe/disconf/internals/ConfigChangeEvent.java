package com.lethe.disconf.internals;

import com.baidu.disconf.client.common.model.DisconfCenterFile;
import com.baidu.disconf.client.fetcher.FetcherMgr;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author : Lethe
 * @version : 1.0
 * @Description :
 * @Date : 2022/11/15 11:43
 * @Copyright : Copyright (c) 2020 All Rights Reserved
 **/
public class ConfigChangeEvent extends ApplicationEvent {

    private static final long serialVersionUID = 4291513694796403089L;

    private String fileName;

    private FetcherMgr fetcherMgr;

    private DisconfCenterFile disconfCenterFile;

    public ConfigChangeEvent(Object source) {
        super(source);
    }

    public ConfigChangeEvent(Object source, FetcherMgr fetcherMgr, DisconfCenterFile disconfCenterFile, String fileName) {
        super(source);
        this.fetcherMgr = fetcherMgr;
        this.fileName = fileName;
        this.disconfCenterFile = disconfCenterFile;
    }


    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public FetcherMgr getFetcherMgr() {
        return fetcherMgr;
    }

    public void setFetcherMgr(FetcherMgr fetcherMgr) {
        this.fetcherMgr = fetcherMgr;
    }

    public DisconfCenterFile getDisconfCenterFile() {
        return disconfCenterFile;
    }

    public void setDisconfCenterFile(DisconfCenterFile disconfCenterFile) {
        this.disconfCenterFile = disconfCenterFile;
    }

}
