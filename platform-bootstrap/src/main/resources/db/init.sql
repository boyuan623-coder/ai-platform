-- volunteer 库初始化：建库、建表、示例数据
CREATE DATABASE IF NOT EXISTS volunteer DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE volunteer;

DROP TABLE IF EXISTS appointment_order;

CREATE TABLE appointment_order (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_name        VARCHAR(64)  NOT NULL COMMENT '用户姓名',
    phone            VARCHAR(20)  NOT NULL COMMENT '手机号',
    service_type     VARCHAR(64)  NOT NULL COMMENT '服务类型',
    appointment_time DATETIME     NOT NULL COMMENT '预约时间',
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/CONFIRMED/CANCELLED/COMPLETED',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_phone (phone),
    INDEX idx_status (status),
    INDEX idx_appointment_time (appointment_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='志愿者预约订单';

INSERT INTO appointment_order (user_name, phone, service_type, appointment_time, status, created_at) VALUES
('张三',   '13800001111', '社区帮扶', '2026-06-28 09:00:00', 'CONFIRMED', '2026-06-20 10:00:00'),
('李四',   '13800002222', '环保志愿', '2026-06-29 14:00:00', 'PENDING',   '2026-06-21 11:30:00'),
('王五',   '13800003333', '敬老助残', '2026-06-30 10:30:00', 'PENDING',   '2026-06-22 09:15:00'),
('赵六',   '13800001111', '科普宣传', '2026-07-01 15:00:00', 'CONFIRMED', '2026-06-23 16:45:00'),
('陈七',   '13900004444', '社区帮扶', '2026-07-02 09:30:00', 'CANCELLED', '2026-06-24 08:20:00'),
('刘八',   '13900005555', '环保志愿', '2026-07-03 13:00:00', 'COMPLETED', '2026-06-18 14:00:00');
