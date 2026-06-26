package com.chatbot.appointment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatbot.appointment.entity.AppointmentOrder;
import com.chatbot.appointment.mapper.AppointmentOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentOrderMapper appointmentOrderMapper;

    public AppointmentOrder create(String userName, String phone, String serviceType, LocalDateTime time) {
        AppointmentOrder order = new AppointmentOrder();
        order.setUserName(userName);
        order.setPhone(phone);
        order.setServiceType(serviceType);
        order.setAppointmentTime(time);
        order.setStatus("PENDING");
        order.setCreatedAt(LocalDateTime.now());
        appointmentOrderMapper.insert(order);
        return order;
    }

    public List<AppointmentOrder> listByPhone(String phone) {
        return appointmentOrderMapper.selectList(
                new LambdaQueryWrapper<AppointmentOrder>()
                        .eq(AppointmentOrder::getPhone, phone)
                        .orderByDesc(AppointmentOrder::getCreatedAt)
        );
    }
}
