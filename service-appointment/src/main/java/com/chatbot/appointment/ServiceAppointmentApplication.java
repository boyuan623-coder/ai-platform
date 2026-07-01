package com.chatbot.appointment;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDubbo
@SpringBootApplication(scanBasePackages = {
    "com.chatbot.appointment",
    "com.chatbot.ai",
    "com.chatbot.rag",
    "com.chatbot.cache",
    "com.chatbot.common",
    "com.chatbot.user.auth"
})
@MapperScan("com.chatbot.appointment.mapper")
@EnableDiscoveryClient
public class ServiceAppointmentApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceAppointmentApplication.class, args);
    }
}
