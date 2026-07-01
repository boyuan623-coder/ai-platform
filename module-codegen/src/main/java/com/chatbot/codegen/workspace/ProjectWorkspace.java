package com.chatbot.codegen.workspace;

import lombok.Data;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

@Component
public class ProjectWorkspace {

    private static final long SESSION_TTL_SECONDS = 7200;

    private final ConcurrentHashMap<String, ProjectSession> sessions = new ConcurrentHashMap<>();

    public ProjectSession getOrCreate(String sessionId) {
        return sessions.computeIfAbsent(sessionId, id -> new ProjectSession());
    }

    public ProjectSession get(String sessionId) {
        return sessions.get(sessionId);
    }

    public void remove(String sessionId) {
        sessions.remove(sessionId);
    }

    @Scheduled(fixedRate = 600000)
    public void evictExpiredSessions() {
        Instant cutoff = Instant.now().minusSeconds(SESSION_TTL_SECONDS);
        sessions.entrySet().removeIf(e -> e.getValue().getCreatedAt().isBefore(cutoff));
    }

    @Data
    public static class ProjectSession {
        private final Instant createdAt = Instant.now();
        private final Map<String, String> files = new ConcurrentHashMap<>();
        private String entryPath = "src/App.vue";
        private String summary = "";
        private volatile BiConsumer<String, String> fileListener;

        public void writeFile(String path, String content) {
            String normalized = normalizePath(path);
            files.put(normalized, content);
            if (fileListener != null) {
                fileListener.accept(normalized, content);
            }
        }

        public String readFile(String path) {
            return files.get(normalizePath(path));
        }

        public Map<String, String> snapshotFiles() {
            return Collections.unmodifiableMap(files);
        }

        public static String normalizePath(String path) {
            String p = path.replace('\\', '/').trim();
            while (p.startsWith("/")) {
                p = p.substring(1);
            }
            return p;
        }
    }
}
