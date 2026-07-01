package com.chatbot.common.util;

import com.chatbot.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SafePathUtilsTest {

    @TempDir
    Path tempDir;

    @Test
    void normalizeRelativePath_rejectsTraversal() {
        assertThrows(BusinessException.class, () -> SafePathUtils.normalizeRelativePath("../etc/passwd"));
        assertThrows(BusinessException.class, () -> SafePathUtils.normalizeRelativePath("a/../b"));
    }

    @Test
    void normalizeRelativePath_stripsLeadingSlash() {
        assertEquals("src/App.vue", SafePathUtils.normalizeRelativePath("/src/App.vue"));
    }

    @Test
    void resolveUnder_allowsValidPath() {
        Path resolved = SafePathUtils.resolveUnder(tempDir, "src/App.vue");
        assertTrue(resolved.startsWith(tempDir.toAbsolutePath().normalize()));
        assertTrue(resolved.toString().replace('\\', '/').endsWith("src/App.vue"));
    }

    @Test
    void resolveUnder_blocksEscape() {
        assertThrows(BusinessException.class, () -> SafePathUtils.resolveUnder(tempDir, "../outside.txt"));
    }
}
