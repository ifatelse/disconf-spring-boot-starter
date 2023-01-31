package com.lethe.disconf.spring.boot;

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

    private String load;

    public String getLoad() {
        return load;
    }

    public void setLoad(String load) {
        this.load = load;
    }

}
