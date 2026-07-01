package com.chatbot.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.cos")
public class CosProperties {

    /** 是否启用 COS 部署（S3 兼容协议） */
    private boolean enabled = false;

    private String endpoint = "";
    private String region = "";
    private String bucket = "";
    private String accessKey = "";
    private String secretKey = "";
    /** 公网访问前缀，如 https://bucket.cos.region.myqcloud.com */
    private String publicUrlPrefix = "";
    /** 对象路径前缀 */
    private String keyPrefix = "deploy";
}
