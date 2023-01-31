package com.lethe.disconf.spring.boot;


import com.lethe.disconf.internals.ConfigChangEventListener;
import com.lethe.disconf.internals.DisconfContextRefresher;
import com.lethe.disconf.registry.DisconfUpdateRegistrar;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Description : Leconf配置中心
 * @Author : liudd12
 * @Date : 2022/8/1 13:51
 * @Version : 1.0
 * @Copyright : Copyright (c) 2022 All Rights Reserved
 **/
@Configuration
// @EnableConfigurationProperties(DisconfProperties.class)
public class DisconfAutoConfiguration {

    public static final String SCAN_PACKAGE = "disconf.config.scan-package";

    public static final String CONF_LOAD = "disconf.config.load";
    public static final String CONF_PREFIX = "classpath:";

    @Bean
    public DisconfProperties disconfProperties() {
        return new DisconfProperties();
    }

    @Bean
    public DisconfUpdateRegistrar disconfUpdateRegistrar() {
        return new DisconfUpdateRegistrar();
    }

    @Bean
    public ConfigChangEventListener changEventListener() {
        return new ConfigChangEventListener();
    }

    @Bean
    public DisconfContextRefresher leconfContextRefresher() {
        return new DisconfContextRefresher();
    }


}
