package com.lethe.disconf.internals;

import com.baidu.disconf.client.common.model.DisconfCenterFile;
import com.baidu.disconf.client.fetcher.FetcherMgr;

/**
 * @Description :
 * @Author : Lethe
 * @Date : 2022/11/14 18:35
 * @Version : 1.0
 * @Copyright : Copyright (c) 2022 All Rights Reserved
 **/
public class RemoteConfigRepository {

    final public FetcherMgr fetcherMgr;

    final public DisconfCenterFile disconfCenterFile;

    public RemoteConfigRepository(FetcherMgr fetcherMgr, DisconfCenterFile disconfCenterFile) {
        this.fetcherMgr = fetcherMgr;
        this.disconfCenterFile = disconfCenterFile;
    }


}
