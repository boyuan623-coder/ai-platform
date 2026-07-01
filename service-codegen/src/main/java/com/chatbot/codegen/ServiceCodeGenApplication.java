package com.chatbot.codegen;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDubbo
@SpringBootApplication(scanBasePackages = {
    "com.chatbot.codegen",
    "com.chatbot.ai",
    "com.chatbot.workflow",
    "com.chatbot.cache",
    "com.chatbot.common",
    "com.chatbot.user.auth"
})
@EnableDiscoveryClient
public class ServiceCodeGenApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceCodeGenApplication.class, args);
    }
}
