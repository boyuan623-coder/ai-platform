package com.chatbot.appointment.tool;

import com.chatbot.appointment.entity.AppointmentOrder;
import com.chatbot.appointment.service.AppointmentService;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AppointmentTools {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AppointmentService appointmentService;

    @Tool("为用户创建服务预约订单，当用户明确要求预约时调用")
    public String createAppointment(String userName, String phone, String serviceType, String appointmentTime) {
        LocalDateTime time = LocalDateTime.parse(appointmentTime, FORMATTER);
        AppointmentOrder order = appointmentService.create(userName, phone, serviceType, time);
        return "预约成功，订单号：" + order.getId() + "，服务：" + serviceType + "，时间：" + appointmentTime;
    }

    @Tool("根据手机号查询用户的预约记录，当用户明确要求查询预约时调用")
    public String queryAppointments(String phone) {
        var orders = appointmentService.listByPhone(phone);
        if (orders.isEmpty()) {
            return "未找到该手机号的预约记录";
        }
        return orders.stream()
                .map(o -> "订单" + o.getId() + " | " + o.getServiceType()
                        + " | " + o.getAppointmentTime().format(FORMATTER)
                        + " | 状态:" + o.getStatus())
                .collect(Collectors.joining("\n"));
    }
}
