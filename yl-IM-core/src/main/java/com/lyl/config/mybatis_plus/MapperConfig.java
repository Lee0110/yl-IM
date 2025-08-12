package com.lyl.config.mybatis_plus;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@MapperScan("com.lyl.mapper")
@Configuration
public class MapperConfig {
}
