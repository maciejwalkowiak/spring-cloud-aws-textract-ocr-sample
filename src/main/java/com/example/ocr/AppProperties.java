package com.example.ocr;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app")
public record AppProperties(String bucket, String baseUrl) {
}
