package com.lethe.disconf.spring.property;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Description :
 * @Author : Lethe
 * @Date : 2022/12/1 18:03
 * @Version : 1.0
 * @Copyright : Copyright (c) 2022 All Rights Reserved
 **/
@Configuration
public class SpringValueAutoConfiguration {


    @Bean
    public SpringValueRegistry springValueRegistry() {
        return new SpringValueRegistry();
    }

    @Bean
    public SpringValueProcessor springValueProcessor(SpringValueRegistry springValueRegistry) {
        return new SpringValueProcessor(springValueRegistry);
    }

    @Bean
    public AutoUpdateValueListener autoUpdateValueListener(SpringValueRegistry springValueRegistry) {
        return new AutoUpdateValueListener(springValueRegistry);
    }

}
