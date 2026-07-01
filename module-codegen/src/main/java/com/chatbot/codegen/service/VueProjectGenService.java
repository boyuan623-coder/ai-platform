package com.chatbot.codegen.service;

import cn.hutool.core.util.IdUtil;
import com.chatbot.codegen.ai.VueProjectAssistant;
import com.chatbot.codegen.dto.VueProjectResult;
import com.chatbot.codegen.tool.VueProjectTools;
import com.chatbot.codegen.workspace.ProjectWorkspace;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VueProjectGenService {

    private final VueProjectAssistant vueProjectAssistant;
    private final ProjectWorkspace workspace;

    public VueProjectResult generate(String requirement) {
        return generate(requirement, IdUtil.fastSimpleUUID());
    }

    public VueProjectResult generate(String requirement, String sessionId) {
        workspace.remove(sessionId);
        VueProjectTools.bindSession(sessionId);
        try {
            String reply = vueProjectAssistant.generateProject(sessionId, requirement);
            VueProjectResult result = buildResult(sessionId, reply);
            ensureHasFiles(result);
            return result;
        } finally {
            VueProjectTools.unbindSession();
        }
    }

    public Flux<String> generateStream(String requirement) {
        return Flux.<String>create(sink -> startVueStream(requirement, sink), FluxSink.OverflowStrategy.BUFFER);
    }

    private void startVueStream(String requirement, FluxSink<String> sink) {
        String sessionId = IdUtil.fastSimpleUUID();
        Thread.startVirtualThread(() -> {
            workspace.remove(sessionId);
            ProjectWorkspace.ProjectSession session = workspace.getOrCreate(sessionId);
            session.setFileListener((path, content) ->
                    safeEmit(sink, "[file:" + path + "]\n" + content + "\n"));

            VueProjectTools.bindSession(sessionId);
            try {
                safeEmit(sink, "[phase:GENERATE] 正在通过 Tool Calling 生成 Vue 工程项目...\n");
                safeEmit(sink, "[session:" + sessionId + "]\n");

                String reply = vueProjectAssistant.generateProject(sessionId, requirement);
                VueProjectResult result = buildResult(sessionId, reply);
                ensureHasFiles(result);

                if (result.getSummary() != null && !result.getSummary().isBlank()) {
                    safeEmit(sink, "[summary]\n" + result.getSummary() + "\n");
                }
                if (result.getAssistantReply() != null && !result.getAssistantReply().isBlank()) {
                    safeEmit(sink, "[reply]\n" + result.getAssistantReply() + "\n");
                }
                safeEmit(sink, "[entry:" + result.getEntryPath() + "]\n");
                safeEmit(sink, "[project:done]\n");
                sink.complete();
            } catch (Exception e) {
                log.error("Vue project generation failed", e);
                safeEmit(sink, "[error]\n" + rootMessage(e) + "\n");
                sink.complete();
            } finally {
                session.setFileListener(null);
                VueProjectTools.unbindSession();
            }
        });
    }

    private void ensureHasFiles(VueProjectResult result) {
        if (result.getFiles() == null || result.getFiles().isEmpty()) {
            throw new IllegalStateException(
                    "模型未通过工具写入任何文件。请检查 DeepSeek API Key 是否有效，或改用「单文件工作流」模式。");
        }
    }

    private static String rootMessage(Throwable e) {
        Throwable cur = e;
        while (cur.getCause() != null) {
            cur = cur.getCause();
        }
        String msg = cur.getMessage();
        return msg != null && !msg.isBlank() ? msg : cur.getClass().getSimpleName();
    }

    private static void safeEmit(FluxSink<String> sink, String chunk) {
        if (!sink.isCancelled()) {
            sink.next(chunk);
        }
    }

    public VueProjectResult getProject(String sessionId) {
        ProjectWorkspace.ProjectSession session = workspace.get(sessionId);
        if (session == null || session.getFiles().isEmpty()) {
            return null;
        }
        return buildResult(sessionId, session.getSummary());
    }

    private VueProjectResult buildResult(String sessionId, String assistantReply) {
        ProjectWorkspace.ProjectSession session = workspace.get(sessionId);
        Map<String, String> files = session != null ? session.snapshotFiles() : Map.of();
        return VueProjectResult.builder()
                .sessionId(sessionId)
                .projectType("vue")
                .entryPath(session != null ? session.getEntryPath() : "src/App.vue")
                .summary(session != null ? session.getSummary() : "")
                .assistantReply(assistantReply)
                .files(files)
                .build();
    }
}
