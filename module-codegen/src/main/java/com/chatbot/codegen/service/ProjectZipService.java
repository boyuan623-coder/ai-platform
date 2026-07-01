package com.chatbot.codegen.service;

import com.chatbot.codegen.dto.VueProjectResult;
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
public class ProjectZipService {

    private final VueProjectGenService vueProjectGenService;

    public byte[] downloadSessionZip(String sessionId) {
        VueProjectResult project = vueProjectGenService.getProject(sessionId);
        if (project == null || project.getFiles() == null || project.getFiles().isEmpty()) {
            throw new BusinessException("项目不存在或已过期");
        }
        return zipFiles(project.getFiles(), "vue-project-" + sessionId);
    }

    public static byte[] zipFiles(Map<String, String> files, String baseName) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(bos)) {
            for (Map.Entry<String, String> entry : files.entrySet()) {
                String path = SafePathUtils.normalizeRelativePath(entry.getKey());
                zos.putNextEntry(new ZipEntry(path));
                zos.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
            zos.finish();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("打包失败: " + e.getMessage());
        }
    }
}
