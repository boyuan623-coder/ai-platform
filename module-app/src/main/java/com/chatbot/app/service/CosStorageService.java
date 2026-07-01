package com.chatbot.app.service;

import com.chatbot.app.config.CosProperties;
import com.chatbot.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * 腾讯云 COS / S3 兼容对象存储上传（可选）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CosStorageService {

    private final CosProperties cosProperties;

    public boolean isEnabled() {
        return cosProperties.isEnabled()
                && cosProperties.getEndpoint() != null
                && !cosProperties.getEndpoint().isBlank()
                && cosProperties.getBucket() != null
                && !cosProperties.getBucket().isBlank();
    }

    public String uploadDirectory(Path localDir, String remotePrefix) {
        if (!isEnabled()) {
            throw new BusinessException("COS 未启用");
        }
        try {
            if (!Files.isDirectory(localDir)) {
                throw new BusinessException("部署目录不存在");
            }
            S3Client client = buildClient();
            try (Stream<Path> walk = Files.walk(localDir)) {
                for (Path file : walk.filter(Files::isRegularFile).toList()) {
                    String relative = localDir.toUri().relativize(file.toUri()).getPath().replace('\\', '/');
                    String key = cosProperties.getKeyPrefix() + "/" + remotePrefix + "/" + relative;
                    client.putObject(
                            PutObjectRequest.builder()
                                    .bucket(cosProperties.getBucket())
                                    .key(key)
                                    .contentType(guessContentType(file.getFileName().toString()))
                                    .build(),
                            RequestBody.fromFile(file)
                    );
                    log.info("COS uploaded: {}", key);
                }
            }
            client.close();
            String prefix = cosProperties.getPublicUrlPrefix().replaceAll("/$", "");
            return prefix + "/" + cosProperties.getKeyPrefix() + "/" + remotePrefix + "/index.html";
        } catch (IOException e) {
            throw new BusinessException("COS 上传失败: " + e.getMessage());
        }
    }

    private S3Client buildClient() {
        return S3Client.builder()
                .endpointOverride(URI.create(cosProperties.getEndpoint()))
                .region(Region.of(cosProperties.getRegion() != null && !cosProperties.getRegion().isBlank()
                        ? cosProperties.getRegion() : "ap-guangzhou"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(cosProperties.getAccessKey(), cosProperties.getSecretKey())))
                .forcePathStyle(true)
                .build();
    }

    private String guessContentType(String name) {
        if (name.endsWith(".html")) return "text/html";
        if (name.endsWith(".css")) return "text/css";
        if (name.endsWith(".js")) return "application/javascript";
        if (name.endsWith(".svg")) return "image/svg+xml";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        return "application/octet-stream";
    }
}
