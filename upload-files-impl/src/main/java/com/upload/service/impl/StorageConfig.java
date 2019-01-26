package com.upload.service.impl;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created by macbookproritena on 1/24/19.
 */
@Component
@ConfigurationProperties("com.upload.service.impl")
public class StorageConfig {

    private String path = "files";
    private long memoryThreshold = 20 * 1_000_000; // 20MB

    public String getPath() {
        Runtime.getRuntime().freeMemory();
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getMemoryThreshold() {
        return memoryThreshold;
    }

    public void setMemoryThreshold(long memoryThreshold) {
        this.memoryThreshold = memoryThreshold;
    }
}