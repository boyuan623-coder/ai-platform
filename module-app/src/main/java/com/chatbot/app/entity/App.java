package com.chatbot.app.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("app")
public class App {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String name;
    private String description;
    private String codeType;
    private String codeContent;
    private String deployUrl;
    private String coverUrl;
    private Boolean isFeatured;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
