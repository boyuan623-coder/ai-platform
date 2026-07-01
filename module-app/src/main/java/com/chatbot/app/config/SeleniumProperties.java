package com.chatbot.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.selenium")
public class SeleniumProperties {

    private boolean enabled = false;
    /** 无头 Chrome 预览基础 URL，如 http://localhost:8080 */
    private String previewBaseUrl = "http://localhost:8080";
    private int viewportWidth = 800;
    private int viewportHeight = 450;
}
