package com.chatbot.codegen.controller;

import com.chatbot.codegen.dto.CodeRunResult;
import com.chatbot.codegen.service.CodeGenService;
import com.chatbot.codegen.service.CodeRunService;
import com.chatbot.common.api.ApiResponse;
import com.chatbot.workflow.CodeGenState;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequestMapping("/api/codegen")
@RequiredArgsConstructor
public class CodeGenController {

    private final CodeGenService codeGenService;
    private final CodeRunService codeRunService;

    @PostMapping("/generate")
    public ApiResponse<CodeGenState> generate(@RequestBody Map<String, String> body) throws Exception {
        return ApiResponse.ok(codeGenService.generate(body.get("requirement")));
    }

    @GetMapping(value = "/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> generateStream(@RequestParam String requirement) {
        return codeGenService.generateStream(requirement)
                .map(chunk -> ServerSentEvent.<String>builder().data(chunk).build());
    }

    @PostMapping("/run")
    public ApiResponse<CodeRunResult> run(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(codeRunService.run(body.get("code"), body.get("language")));
    }
}
