package com.lethe.disconf.boot;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @Description :
 * @Author : liudd12
 * @Date : 2022/11/4 14:33
 * @Version : 1.0
 * @Copyright : Copyright (c) 2022 All Rights Reserved
 **/
@ConfigurationProperties("disconf.config")
public class DisconfProperties {

    public static final String CONF_LOAD = "disconf.config.load";
    public static final String CONF_PREFIX = "classpath:";

    private String load;

    public String getLoad() {
        return load;
    }

    public void setLoad(String load) {
        this.load = load;
    }

}
