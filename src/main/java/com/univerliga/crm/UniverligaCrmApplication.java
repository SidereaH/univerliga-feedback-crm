package com.univerliga.crm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class UniverligaCrmApplication {

    public static void main(String[] args) {
        SpringApplication.run(UniverligaCrmApplication.class, args);
    }
}
