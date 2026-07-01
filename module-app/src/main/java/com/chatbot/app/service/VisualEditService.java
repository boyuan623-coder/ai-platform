package com.chatbot.app.service;

import com.chatbot.app.dto.AppProjectPayload;
import com.chatbot.app.dto.VisualEditRequest;
import com.chatbot.app.entity.App;
import com.chatbot.common.exception.BusinessException;
import com.chatbot.common.util.SafePathUtils;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class VisualEditService {

    private static final Pattern FENCE = Pattern.compile("```[\\w]*\\s*([\\s\\S]*?)```", Pattern.MULTILINE);

    private final AppService appService;
    private final AppDeployService appDeployService;
    private final ChatModel chatModel;

    public App edit(String token, Long appId, VisualEditRequest request) {
        if (request.getInstruction() == null || request.getInstruction().isBlank()) {
            throw new BusinessException("请描述要如何修改");
        }
        App app = appService.requireOwnedApp(token, appId);
        AppProjectPayload payload = AppProjectPayload.fromCodeContent(app.getCodeContent(), app.getCodeType());
        if (payload == null || payload.getFiles().isEmpty()) {
            throw new BusinessException("应用暂无代码，请先生成项目");
        }

        String filePath = resolveFilePath(payload, request);
        String original = payload.getFiles().get(filePath);
        if (original == null) {
            throw new BusinessException("找不到可编辑的文件: " + filePath);
        }

        String prompt = """
                你是前端可视化编辑专家。根据用户对页面元素的修改要求，输出修改后的【完整文件内容】。
                要求：保持文件可运行；只改与需求相关的部分；不要省略代码；不要使用 markdown 代码块包裹输出。
                
                文件路径：%s
                选中元素选择器：%s
                选中元素文本：%s
                用户修改要求：%s
                
                原始文件内容：
                %s
                """.formatted(
                filePath,
                nullToEmpty(request.getElementSelector()),
                nullToEmpty(request.getElementText()),
                request.getInstruction().trim(),
                original
        );

        String updated = stripFence(chatModel.chat(prompt));
        if (updated.isBlank()) {
            throw new BusinessException("AI 未返回有效内容");
        }
        payload.getFiles().put(filePath, updated);
        appService.saveProject(token, appId, payload);

        if ("DEPLOYED".equals(app.getStatus())) {
            return appDeployService.deploy(token, appId);
        }
        return appService.getById(token, appId);
    }

    private String resolveFilePath(AppProjectPayload payload, VisualEditRequest request) {
        if (request.getFilePath() != null && !request.getFilePath().isBlank()) {
            return SafePathUtils.normalizeRelativePath(request.getFilePath());
        }
        if (payload.getEntryPath() != null && payload.getFiles().containsKey(payload.getEntryPath())) {
            return payload.getEntryPath();
        }
        if (payload.getFiles().containsKey("index.html")) {
            return "index.html";
        }
        if (payload.getFiles().containsKey("src/App.vue")) {
            return "src/App.vue";
        }
        return payload.getFiles().keySet().iterator().next();
    }

    private static String stripFence(String text) {
        Matcher m = FENCE.matcher(text.trim());
        if (m.find()) {
            return m.group(1).trim();
        }
        return text.trim();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
