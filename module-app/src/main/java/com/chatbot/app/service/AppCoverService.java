package com.chatbot.app.service;

import com.chatbot.app.config.AppStorageProperties;
import com.chatbot.app.dto.AppProjectPayload;
import com.chatbot.app.entity.App;
import com.chatbot.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AppCoverService {

    private final AppStorageProperties storageProperties;
    private final AppService appService;
    private final AppCoverScreenshotService screenshotService;

    public App generateCover(String token, Long appId) {
        App app = appService.requireOwnedApp(token, appId);

        if ("DEPLOYED".equals(app.getStatus()) && screenshotService.isEnabled()) {
            boolean ok = screenshotService.capturePreview(appId, "/api/app/preview/" + appId);
            if (ok) {
                app.setCoverUrl("/api/app/" + appId + "/cover/image?type=png");
                app.setUpdatedAt(LocalDateTime.now());
                return appService.updateDirect(app);
            }
        }

        String svg = buildCoverSvg(app);
        Path coverFile = coverPath(appId);
        try {
            Files.createDirectories(coverFile.getParent());
            Files.writeString(coverFile, svg, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BusinessException("封面生成失败: " + e.getMessage());
        }
        app.setCoverUrl("/api/app/" + appId + "/cover/image");
        app.setUpdatedAt(LocalDateTime.now());
        return appService.updateDirect(app);
    }

    public Path getCoverFile(Long appId) {
        App app = appService.getAppEntity(appId);
        if (app.getCoverUrl() == null || app.getCoverUrl().isBlank()) {
            throw new BusinessException("封面不存在，请先生成封面");
        }
        if (app.getCoverUrl().contains("type=png")) {
            Path png = screenshotService.coverPngPath(appId);
            if (Files.exists(png)) {
                return png;
            }
        }
        Path path = coverPath(appId);
        if (!Files.exists(path)) {
            throw new BusinessException("封面不存在，请先生成封面");
        }
        return path;
    }

    private Path coverPath(Long appId) {
        return Path.of(storageProperties.getBasePath(), "covers", appId + ".svg");
    }

    private String buildCoverSvg(App app) {
        AppProjectPayload payload = AppProjectPayload.fromCodeContent(app.getCodeContent(), app.getCodeType());
        String subtitle = app.getDescription() != null && !app.getDescription().isBlank()
                ? app.getDescription()
                : (payload != null ? payload.getProjectType() : app.getCodeType()) + " 应用";

        String name = escapeXml(app.getName());
        String desc = escapeXml(truncate(subtitle, 60));
        int hash = Math.abs(app.getName().hashCode());
        String color1 = colorFromHash(hash, 0);
        String color2 = colorFromHash(hash, 1);

        return """
                <svg xmlns="http://www.w3.org/2000/svg" width="800" height="450" viewBox="0 0 800 450">
                  <defs>
                    <linearGradient id="g" x1="0%" y1="0%" x2="100%" y2="100%">
                      <stop offset="0%" style="stop-color:%s"/>
                      <stop offset="100%" style="stop-color:%s"/>
                    </linearGradient>
                  </defs>
                  <rect width="800" height="450" fill="url(#g)"/>
                  <text x="48" y="200" fill="#ffffff" font-family="system-ui,sans-serif" font-size="42" font-weight="700">%s</text>
                  <text x="48" y="250" fill="#e2e8f0" font-family="system-ui,sans-serif" font-size="18">%s</text>
                  <text x="48" y="400" fill="#cbd5e1" font-family="system-ui,sans-serif" font-size="14">AI 零代码平台 · 应用封面</text>
                </svg>
                """.formatted(color1, color2, name, desc);
    }

    private static String colorFromHash(int hash, int offset) {
        int r = (hash >> (offset * 3)) & 0xFF;
        int g = (hash >> (offset * 3 + 8)) & 0xFF;
        int b = (hash >> (offset * 3 + 16)) & 0xFF;
        r = 80 + (r % 120);
        g = 80 + (g % 120);
        b = 120 + (b % 100);
        return String.format("#%02x%02x%02x", r, g, b);
    }

    private static String escapeXml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    private static String truncate(String s, int max) {
        if (s.length() <= max) return s;
        return s.substring(0, max - 1) + "…";
    }
}
