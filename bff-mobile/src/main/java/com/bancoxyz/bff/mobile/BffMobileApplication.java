package com.bancoxyz.bff.mobile;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class BffMobileApplication {
    public static void main(String[] args) {
        SpringApplication.run(BffMobileApplication.class, args);
    }
}
