package com.chatbot.workflow;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public final class LlmStreamHelper {

    private static final long TIMEOUT_MINUTES = 8;

    private LlmStreamHelper() {}

    public static String chatStreamCollect(
            StreamingChatModel model,
            String prompt,
            Consumer<String> onToken) throws InterruptedException {
        AtomicReference<String> result = new AtomicReference<>("");
        AtomicReference<Throwable> error = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        model.chat(prompt, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                if (partialResponse != null && onToken != null) {
                    onToken.accept(partialResponse);
                }
            }

            @Override
            public void onCompleteResponse(ChatResponse response) {
                if (response != null && response.aiMessage() != null) {
                    result.set(response.aiMessage().text());
                }
                latch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                error.set(throwable);
                latch.countDown();
            }
        });

        if (!latch.await(TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
            throw new RuntimeException("LLM 响应超时（超过 " + TIMEOUT_MINUTES + " 分钟）");
        }
        if (error.get() != null) {
            throw new RuntimeException(error.get());
        }
        return result.get();
    }
}
