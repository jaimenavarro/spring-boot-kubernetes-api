package com.example.demo.config;

import lombok.Data;

@Data
public class DataProperty {
    private String route;
    private String throttlingKey;
    private Integer throttlingLimit;
}
