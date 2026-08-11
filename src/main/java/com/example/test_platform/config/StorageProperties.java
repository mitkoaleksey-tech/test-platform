package com.example.test_platform.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.storage")
@Getter
@Setter
public class StorageProperties {

    private String imagesPath = "./storage/images";
    private int imageTargetWidth = 800;
    private String publicUrlPrefix = "/api/public/images";
}
