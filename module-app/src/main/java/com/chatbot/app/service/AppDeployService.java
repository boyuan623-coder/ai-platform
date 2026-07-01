package com.chatbot.app.service;

import com.chatbot.app.config.AppStorageProperties;
import com.chatbot.app.dto.AppProjectPayload;
import com.chatbot.app.entity.App;
import com.chatbot.common.exception.BusinessException;
import com.chatbot.common.util.SafePathUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AppDeployService {

    private final AppStorageProperties storageProperties;
    private final AppService appService;
    private final CosStorageService cosStorageService;

    public App deploy(String token, Long appId) {
        App app = appService.requireOwnedApp(token, appId);
        AppProjectPayload payload = AppProjectPayload.fromCodeContent(app.getCodeContent(), app.getCodeType());
        if (payload == null || payload.getFiles().isEmpty()) {
            throw new BusinessException("应用暂无代码，无法部署");
        }

        Path deployDir = deployPath(appId);
        try {
            if (Files.exists(deployDir)) {
                deleteRecursively(deployDir);
            }
            Files.createDirectories(deployDir);
            for (Map.Entry<String, String> entry : payload.getFiles().entrySet()) {
                Path file = SafePathUtils.resolveUnder(deployDir, entry.getKey());
                Files.createDirectories(file.getParent());
                Files.writeString(file, entry.getValue(), StandardCharsets.UTF_8);
            }
            ensureIndexHtml(deployDir, payload);
        } catch (IOException e) {
            throw new BusinessException("部署失败: " + e.getMessage());
        }

        app.setDeployUrl("/api/app/preview/" + appId);
        app.setStatus("DEPLOYED");
        app.setUpdatedAt(LocalDateTime.now());
        app = appService.updateDirect(app);

        if (cosStorageService.isEnabled()) {
            try {
                String publicUrl = cosStorageService.uploadDirectory(deployDir, String.valueOf(appId));
                app.setDeployUrl(publicUrl);
                app.setUpdatedAt(LocalDateTime.now());
                app = appService.updateDirect(app);
            } catch (Exception e) {
                throw new BusinessException("本地部署成功但 COS 同步失败: " + e.getMessage());
            }
        }
        return app;
    }

    public Path deployDirectory(Long appId) {
        return deployPath(appId);
    }

    public Path resolvePreviewFile(Long appId, String relativePath) {
        App app = appService.getAppEntity(appId);
        if (!"DEPLOYED".equals(app.getStatus())) {
            throw new BusinessException("应用尚未部署");
        }
        Path deployDir = deployPath(appId);
        if (!Files.exists(deployDir)) {
            throw new BusinessException("部署目录不存在");
        }
        String safe = relativePath == null || relativePath.isBlank() ? "index.html" : relativePath;
        Path file = SafePathUtils.resolveUnder(deployDir, safe);
        if (!Files.exists(file)) {
            throw new BusinessException("资源不存在: " + safe);
        }
        return file;
    }

    private void ensureIndexHtml(Path deployDir, AppProjectPayload payload) throws IOException {
        Path index = deployDir.resolve("index.html");
        if (Files.exists(index)) {
            return;
        }
        String entry = payload.getEntryPath() != null ? payload.getEntryPath() : "index.html";
        String content = payload.getFiles().get(entry);
        if (content == null && entry.endsWith(".vue")) {
            content = payload.getFiles().get("src/App.vue");
        }
        if (content == null) {
            content = "<html><body><h1>Deployed</h1></body></html>";
        } else if (!entry.equals("index.html") && !content.trim().startsWith("<")) {
            content = wrapVueAsHtml(content);
        }
        Files.writeString(index, content, StandardCharsets.UTF_8);
    }

    private String wrapVueAsHtml(String vueOrHtml) {
        if (vueOrHtml == null) {
            return "<html><body><p>Empty</p></body></html>";
        }
        String trimmed = vueOrHtml.trim();
        if (trimmed.startsWith("<!DOCTYPE") || trimmed.startsWith("<html")) {
            return vueOrHtml;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("<template[^>]*>([\\s\\S]*?)</template>", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(vueOrHtml);
        String template = m.find() ? m.group(1).trim() : "<p>Vue App</p>";
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><style>body{margin:0;padding:16px;font-family:sans-serif}</style></head><body>"
                + template + "</body></html>";
    }

    private Path deployPath(Long appId) {
        return Path.of(storageProperties.getBasePath(), "deploy", String.valueOf(appId));
    }

    private void deleteRecursively(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (var stream = Files.list(path)) {
                for (Path child : stream.toList()) {
                    deleteRecursively(child);
                }
            }
        }
        Files.deleteIfExists(path);
    }
}
