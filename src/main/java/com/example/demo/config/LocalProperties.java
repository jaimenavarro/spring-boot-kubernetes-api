package com.example.demo.config;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "configuration")
@Data
public class LocalProperties {
    private String namespace;
    private String lables;
    private String apigwNamespace;
    private String apigwLables;
    private List<Map<String,DataProperty>> apigwProperties;
}
