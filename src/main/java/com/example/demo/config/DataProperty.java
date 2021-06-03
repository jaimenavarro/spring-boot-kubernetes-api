package com.example.demo.config;

import lombok.Data;

@Data
public class DataProperty {
    private String name;
    private Integer limitPerPod;
    private Boolean dinamicLimit;
    private Integer totalLimit;
    private Integer totalLimitPerApiGw;
}
