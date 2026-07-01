package com.chatbot.app.service;

import com.chatbot.app.dto.AppProjectPayload;
import com.chatbot.app.entity.App;
import com.chatbot.common.exception.BusinessException;
import com.chatbot.common.util.SafePathUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class AppPackageService {

    private final AppService appService;

    public byte[] downloadZip(String token, Long appId) {
        App app = appService.getById(token, appId);
        AppProjectPayload payload = AppProjectPayload.fromCodeContent(app.getCodeContent(), app.getCodeType());
        if (payload == null || payload.getFiles().isEmpty()) {
            throw new BusinessException("应用暂无代码可下载");
        }
        return zipFiles(payload.getFiles(), app.getName());
    }

    public static byte[] zipFiles(Map<String, String> files, String projectName) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(bos)) {
            for (Map.Entry<String, String> entry : files.entrySet()) {
                String path = SafePathUtils.normalizeRelativePath(entry.getKey());
                ZipEntry zipEntry = new ZipEntry(path);
                zos.putNextEntry(zipEntry);
                byte[] bytes = entry.getValue().getBytes(StandardCharsets.UTF_8);
                zos.write(bytes);
                zos.closeEntry();
            }
            zos.finish();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("打包失败: " + e.getMessage());
        }
    }

    public static String safeZipName(String name) {
        String safe = name.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5_-]", "_");
        if (safe.isBlank()) {
            return "project";
        }
        return safe;
    }
}
