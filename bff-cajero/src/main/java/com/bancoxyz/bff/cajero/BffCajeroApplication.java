package com.bancoxyz.bff.cajero;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class BffCajeroApplication {
    public static void main(String[] args) {
        SpringApplication.run(BffCajeroApplication.class, args);
    }
}
