package com.chatbot.ai.support;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.Callable;

/**
 * 在 WebFlux 事件循环上调用阻塞式 LLM / JDBC 等 API 时，必须先切到专用线程池。
 */
public final class ReactiveBlocking {

    private static final Scheduler SCHEDULER = Schedulers.newBoundedElastic(
            Math.max(8, Runtime.getRuntime().availableProcessors() * 2),
            10_000,
            "blocking-io"
    );

    private ReactiveBlocking() {
    }

    public static <T> Mono<T> mono(Callable<T> callable) {
        return Mono.fromCallable(callable).subscribeOn(SCHEDULER);
    }

    public static Scheduler scheduler() {
        return SCHEDULER;
    }
}
