package com.lyl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class YlIMApplication {
    public static void main(String[] args) {
        SpringApplication.run(YlIMApplication.class, args);
    }
}
