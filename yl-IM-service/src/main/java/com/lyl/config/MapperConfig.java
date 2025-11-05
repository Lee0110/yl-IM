package com.lyl.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@MapperScan("com.lyl.service.*.mapper")
@Configuration
public class MapperConfig {
}
