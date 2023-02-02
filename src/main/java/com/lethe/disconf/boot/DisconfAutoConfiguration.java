package com.lethe.disconf.boot;

import com.lethe.disconf.internals.DisconfContextRefresher;
import com.lethe.disconf.registry.DisconfUpdateRegistrar;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * @Description : Leconf配置中心
 * @Author : liudd12
 * @Date : 2022/8/1 13:51
 * @Version : 1.0
 * @Copyright : Copyright (c) 2022 All Rights Reserved
 **/
@AutoConfiguration
@EnableConfigurationProperties(DisconfProperties.class)
@ConditionalOnProperty(name = "disconf.config.refresh-enabled", havingValue = "true", matchIfMissing = true)
public class DisconfAutoConfiguration {


    @Bean
    public DisconfUpdateRegistrar disconfUpdateRegistrar() {
        return new DisconfUpdateRegistrar();
    }

    @Bean
    public DisconfContextRefresher disconfContextRefresher() {
        return new DisconfContextRefresher();
    }

}
