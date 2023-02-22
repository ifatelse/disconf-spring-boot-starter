package com.lethe.disconf.internals;

import org.springframework.context.ApplicationEvent;

/**
 * @Description :
 * @Author : Lethe
 * @Date : 2023/2/22 10:04
 * @Version : 1.0
 * @Copyright : Copyright (c) 2023 All Rights Reserved
 **/
public class ConfigChangeEvent  extends ApplicationEvent {

    private static final long serialVersionUID = 6496480185283581749L;

    private final String fileName;

    public ConfigChangeEvent(Object source, String fileName) {
        super(source);
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }
}
