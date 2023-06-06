package com.lethe.disconf.configdata;

import org.springframework.boot.context.config.*;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * @Description :
 * @Author : Lethe
 * @Date : 2023/6/2 14:16
 * @Version : 1.0
 * @Copyright : Copyright (c) 2023 All Rights Reserved
 **/
public class DisconfConfigDataLocationResolver implements ConfigDataLocationResolver<DisconfConfigDataResource>, Ordered {

    /**
     * Prefix for Config Server imports.
     */
    public static final String PREFIX = "disconf:";


    @Override
    public boolean isResolvable(ConfigDataLocationResolverContext context, ConfigDataLocation location) {
        return location.hasPrefix(PREFIX);
    }

    @Override
    public List<DisconfConfigDataResource> resolve(ConfigDataLocationResolverContext context,
                                                   ConfigDataLocation location)
            throws ConfigDataLocationNotFoundException, ConfigDataResourceNotFoundException {
        return Collections.emptyList();
    }


    @Override
    public List<DisconfConfigDataResource> resolveProfileSpecific(ConfigDataLocationResolverContext context,
                                                                  ConfigDataLocation location, Profiles profiles)
            throws ConfigDataLocationNotFoundException {
        String confName = location.getNonPrefixedValue(PREFIX);
        if (StringUtils.hasText(confName)) {
            return Collections.singletonList(new DisconfConfigDataResource(location.isOptional(), confName));
        }
        return Collections.emptyList();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
