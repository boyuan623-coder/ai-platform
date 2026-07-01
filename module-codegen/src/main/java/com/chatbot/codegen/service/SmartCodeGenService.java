package com.chatbot.codegen.service;

import com.chatbot.ai.support.ReactiveBlocking;
import com.chatbot.codegen.dto.RouteResult;
import com.chatbot.codegen.dto.VueProjectResult;
import com.chatbot.codegen.enums.CodegenStrategy;
import com.chatbot.workflow.CodeGenState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class SmartCodeGenService {

    private final AiRouteService aiRouteService;
    private final VueProjectGenService vueProjectGenService;
    private final CodeGenService codeGenService;

    public Mono<RouteResult> routeAsync(String requirement) {
        return ReactiveBlocking.mono(() -> aiRouteService.route(requirement));
    }

    public RouteResult route(String requirement) {
        return aiRouteService.route(requirement);
    }

    public Mono<Object> generateAsync(String requirement) {
        return ReactiveBlocking.mono(() -> generate(requirement));
    }

    public Object generate(String requirement) throws Exception {
        RouteResult route = aiRouteService.route(requirement);
        return switch (route.getStrategy()) {
            case VUE_PROJECT -> vueProjectGenService.generate(requirement);
            case HTML_PAGE -> wrapHtmlResult(codeGenService.generate(wrapHtmlRequirement(requirement)));
            case SINGLE_CODE -> codeGenService.generate(requirement);
        };
    }

    public Flux<String> generateStream(String requirement) {
        return ReactiveBlocking.mono(() -> aiRouteService.route(requirement))
                .flatMapMany(route -> {
                    Flux<String> header = Flux.just(
                            "[route:" + route.getStrategy().name() + "]\n",
                            "[route-reason]" + route.getReason() + "\n"
                    );
                    Flux<String> downstream = switch (route.getStrategy()) {
                        case VUE_PROJECT -> Flux.defer(() -> vueProjectGenService.generateStream(requirement));
                        case HTML_PAGE -> Flux.defer(() -> codeGenService.generateStream(wrapHtmlRequirement(requirement)));
                        case SINGLE_CODE -> Flux.defer(() -> codeGenService.generateStream(requirement));
                    };
                    return header.concatWith(downstream);
                })
                .onErrorResume(e -> Flux.just("[error]\n" + e.getMessage() + "\n"));
    }

    private String wrapHtmlRequirement(String requirement) {
        return "生成一个完整的单文件 HTML 页面（含 CSS/JS），需求：" + requirement;
    }

    private VueProjectResult wrapHtmlResult(CodeGenState state) {
        String code = state.optimizedCode() != null ? state.optimizedCode() : state.generatedCode();
        return VueProjectResult.builder()
                .projectType("html")
                .entryPath("index.html")
                .summary(state.analysis())
                .files(code != null ? Map.of("index.html", code) : Map.of())
                .build();
    }
}
