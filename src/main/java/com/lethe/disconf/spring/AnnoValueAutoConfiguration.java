package com.lethe.disconf.spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Description :
 * @Author : liudd12
 * @Date : 2022/12/1 18:03
 * @Version : 1.0
 * @Copyright : Copyright (c) 2022 All Rights Reserved
 **/
@Configuration
public class AnnoValueAutoConfiguration {


    @Bean
    public AnnoValueRegistry springValueRegistry() {
        return new AnnoValueRegistry();
    }

    @Bean
    public AnnoValueProcessor springValueProcessor(AnnoValueRegistry annoValueRegistry) {
        return new AnnoValueProcessor(annoValueRegistry);
    }

    @Bean
    public AutoUpdateValueListener autoUpdateValueListener(AnnoValueRegistry annoValueRegistry) {
        return new AutoUpdateValueListener(annoValueRegistry);
    }

}
