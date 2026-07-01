package com.chatbot.common.util;

import com.chatbot.common.exception.BusinessException;

import java.nio.file.Path;

public final class SafePathUtils {

    private SafePathUtils() {}

    public static String normalizeRelativePath(String path) {
        if (path == null || path.isBlank()) {
            throw new BusinessException("非法文件路径");
        }
        String normalized = path.replace('\\', '/').trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.contains("..") || normalized.contains(":")) {
            throw new BusinessException("非法文件路径: " + path);
        }
        return normalized;
    }

    public static Path resolveUnder(Path baseDir, String relativePath) {
        Path base = baseDir.toAbsolutePath().normalize();
        Path resolved = base.resolve(normalizeRelativePath(relativePath)).normalize();
        if (!resolved.startsWith(base)) {
            throw new BusinessException("非法文件路径: " + relativePath);
        }
        return resolved;
    }
}
