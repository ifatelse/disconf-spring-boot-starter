package com.lethe.disconf.configdata;

import org.springframework.boot.context.config.ConfigDataResource;

import java.util.Objects;

/**
 * @Description :
 * @Author : Lethe
 * @Date : 2023/6/2 14:15
 * @Version : 1.0
 * @Copyright : Copyright (c) 2023 All Rights Reserved
 **/
public class DisconfConfigDataResource extends ConfigDataResource {

    private final boolean optional;

    private final String confName;

    public DisconfConfigDataResource(boolean optional, String confName) {
        this.optional = optional;
        this.confName = confName;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DisconfConfigDataResource that = (DisconfConfigDataResource) o;
        return optional == that.optional && Objects.equals(confName, that.confName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(optional, confName);
    }

    public boolean isOptional() {
        return optional;
    }

    public String getConfName() {
        return confName;
    }


}
