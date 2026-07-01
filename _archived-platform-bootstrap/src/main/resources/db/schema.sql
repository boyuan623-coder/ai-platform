CREATE DATABASE IF NOT EXISTS volunteer DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE volunteer;

CREATE TABLE IF NOT EXISTS appointment_order (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_name        VARCHAR(64)  NOT NULL COMMENT '用户姓名',
    phone            VARCHAR(20)  NOT NULL COMMENT '手机号',
    service_type     VARCHAR(64)  NOT NULL COMMENT '服务类型',
    appointment_time DATETIME     NOT NULL COMMENT '预约时间',
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '状态',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='志愿者预约订单';
