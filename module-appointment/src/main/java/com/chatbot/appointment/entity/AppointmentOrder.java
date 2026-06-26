package com.chatbot.appointment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("appointment_order")
public class AppointmentOrder {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String userName;
    private String phone;
    private String serviceType;
    private LocalDateTime appointmentTime;
    private String status;
    private LocalDateTime createdAt;
}
