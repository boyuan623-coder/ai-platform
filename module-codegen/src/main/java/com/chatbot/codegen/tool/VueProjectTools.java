package com.chatbot.codegen.tool;

import com.chatbot.codegen.workspace.ProjectWorkspace;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class VueProjectTools {

    private static final ThreadLocal<String> SESSION = new ThreadLocal<>();

    private final ProjectWorkspace workspace;

    public static void bindSession(String sessionId) {
        SESSION.set(sessionId);
    }

    public static void unbindSession() {
        SESSION.remove();
    }

    private String sessionId() {
        String id = SESSION.get();
        if (id == null) {
            throw new IllegalStateException("代码生成会话未初始化");
        }
        return id;
    }

    @Tool("创建或覆盖 Vue 工程项目中的文件，path 使用相对路径如 src/App.vue、package.json")
    public String writeFile(String path, String content) {
        ProjectWorkspace.ProjectSession session = workspace.getOrCreate(sessionId());
        session.writeFile(path, content);
        return "已写入文件: " + path + "（" + content.length() + " 字符）";
    }

    @Tool("读取项目中已存在的文件内容")
    public String readFile(String path) {
        ProjectWorkspace.ProjectSession session = workspace.get(sessionId());
        if (session == null) {
            return "会话不存在";
        }
        String content = session.readFile(path);
        if (content == null) {
            return "文件不存在: " + path;
        }
        return content;
    }

    @Tool("列出当前项目中所有已创建的文件路径")
    public String listFiles() {
        ProjectWorkspace.ProjectSession session = workspace.get(sessionId());
        if (session == null || session.getFiles().isEmpty()) {
            return "暂无文件";
        }
        return session.getFiles().keySet().stream()
                .sorted()
                .collect(Collectors.joining("\n"));
    }

    @Tool("标记 Vue 工程项目生成完成，并指定入口文件路径")
    public String finalizeProject(String entryPath, String summary) {
        ProjectWorkspace.ProjectSession session = workspace.getOrCreate(sessionId());
        session.setEntryPath(ProjectWorkspace.ProjectSession.normalizePath(entryPath));
        session.setSummary(summary);
        return "项目已完成，入口: " + entryPath;
    }
}
