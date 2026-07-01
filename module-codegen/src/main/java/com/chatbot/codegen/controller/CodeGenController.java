package com.chatbot.codegen.controller;

import com.chatbot.codegen.dto.CodeRunResult;
import com.chatbot.codegen.dto.RouteResult;
import com.chatbot.codegen.dto.VueProjectResult;
import com.chatbot.codegen.service.CodeGenService;
import com.chatbot.codegen.service.CodeRunService;
import com.chatbot.codegen.service.ProjectZipService;
import com.chatbot.codegen.service.SmartCodeGenService;
import com.chatbot.codegen.service.VueProjectGenService;
import com.chatbot.ai.support.ReactiveBlocking;
import com.chatbot.common.api.ApiResponse;
import com.chatbot.user.auth.TokenAuthSupport;
import com.chatbot.workflow.CodeGenState;
import com.chatbot.workflow.WorkflowGraphDefinition;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/codegen")
@RequiredArgsConstructor
public class CodeGenController {

    private final CodeGenService codeGenService;
    private final VueProjectGenService vueProjectGenService;
    private final SmartCodeGenService smartCodeGenService;
    private final ProjectZipService projectZipService;
    private final CodeRunService codeRunService;
    private final TokenAuthSupport tokenAuthSupport;

    private void requireAuth(String token) {
        tokenAuthSupport.requireAuth(token);
    }

    @GetMapping("/workflow/graph")
    public ApiResponse<Map<String, Object>> workflowGraph(
            @RequestHeader("X-Auth-Token") String token) {
        requireAuth(token);
        return ApiResponse.ok(WorkflowGraphDefinition.asMap());
    }

    @PostMapping("/generate")
    public Mono<ApiResponse<CodeGenState>> generate(
            @RequestHeader("X-Auth-Token") String token,
            @RequestBody Map<String, String> body) {
        requireAuth(token);
        return ReactiveBlocking.mono(() -> codeGenService.generate(body.get("requirement")))
                .map(ApiResponse::ok);
    }

    private Flux<ServerSentEvent<String>> toSse(Flux<String> stream) {
        return stream
                .onErrorResume(e -> Flux.just("[error]\n" + e.getMessage() + "\n"))
                .map(chunk -> ServerSentEvent.<String>builder().data(chunk).build());
    }

    @GetMapping(value = "/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> generateStream(
            @RequestHeader("X-Auth-Token") String token,
            @RequestParam String requirement) {
        requireAuth(token);
        return toSse(codeGenService.generateStream(requirement));
    }

    @PostMapping("/run")
    public ApiResponse<CodeRunResult> run(
            @RequestHeader("X-Auth-Token") String token,
            @RequestBody Map<String, String> body) {
        requireAuth(token);
        return ApiResponse.ok(codeRunService.run(body.get("code"), body.get("language")));
    }

    @PostMapping("/vue/generate")
    public Mono<ApiResponse<VueProjectResult>> generateVueProject(
            @RequestHeader("X-Auth-Token") String token,
            @RequestBody Map<String, String> body) {
        requireAuth(token);
        return ReactiveBlocking.mono(() -> vueProjectGenService.generate(body.get("requirement")))
                .map(ApiResponse::ok);
    }

    @GetMapping(value = "/vue/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> generateVueProjectStream(
            @RequestHeader("X-Auth-Token") String token,
            @RequestParam String requirement) {
        requireAuth(token);
        return toSse(vueProjectGenService.generateStream(requirement));
    }

    @GetMapping("/vue/project/{sessionId}")
    public ApiResponse<VueProjectResult> getVueProject(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable String sessionId) {
        requireAuth(token);
        VueProjectResult result = vueProjectGenService.getProject(sessionId);
        if (result == null) {
            return ApiResponse.fail("项目不存在或已过期");
        }
        return ApiResponse.ok(result);
    }

    @GetMapping("/vue/download/{sessionId}")
    public Mono<ResponseEntity<byte[]>> downloadVueProject(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable String sessionId) {
        return Mono.fromCallable(() -> {
            requireAuth(token);
            byte[] zip = projectZipService.downloadSessionZip(sessionId);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"vue-project.zip\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(zip);
        });
    }

    @PostMapping("/route")
    public Mono<ApiResponse<RouteResult>> route(
            @RequestHeader("X-Auth-Token") String token,
            @RequestBody Map<String, String> body) {
        requireAuth(token);
        return smartCodeGenService.routeAsync(body.get("requirement"))
                .map(ApiResponse::ok);
    }

    @PostMapping("/smart/generate")
    public Mono<ApiResponse<Object>> smartGenerate(
            @RequestHeader("X-Auth-Token") String token,
            @RequestBody Map<String, String> body) {
        requireAuth(token);
        return smartCodeGenService.generateAsync(body.get("requirement"))
                .map(ApiResponse::ok);
    }

    @GetMapping(value = "/smart/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> smartGenerateStream(
            @RequestHeader("X-Auth-Token") String token,
            @RequestParam String requirement) {
        requireAuth(token);
        return toSse(smartCodeGenService.generateStream(requirement));
    }
}
