-- ai_platform 库初始化：建库、建表、示例数据
CREATE DATABASE IF NOT EXISTS ai_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE ai_platform;

DROP TABLE IF EXISTS chat_message;
DROP TABLE IF EXISTS assistant_message;
DROP TABLE IF EXISTS assistant_session;
DROP TABLE IF EXISTS app;
DROP TABLE IF EXISTS user;
DROP TABLE IF EXISTS appointment_order;

CREATE TABLE user (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    username     VARCHAR(64)  NOT NULL,
    password     VARCHAR(128) NOT NULL,
    nickname     VARCHAR(64)  DEFAULT NULL,
    avatar       VARCHAR(512) DEFAULT NULL,
    role         VARCHAR(20)  NOT NULL DEFAULT 'USER',
    status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE app (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    name         VARCHAR(128) NOT NULL,
    description  VARCHAR(512) DEFAULT NULL,
    code_type    VARCHAR(32)  NOT NULL DEFAULT 'HTML',
    code_content MEDIUMTEXT   DEFAULT NULL,
    deploy_url   VARCHAR(512) DEFAULT NULL,
    cover_url    VARCHAR(512) DEFAULT NULL,
    is_featured  TINYINT(1)   NOT NULL DEFAULT 0,
    status       VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_featured (is_featured, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE assistant_session (
    id           VARCHAR(64)  PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    title        VARCHAR(128) NOT NULL DEFAULT '新对话',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_updated (user_id, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE assistant_message (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id   VARCHAR(64)  NOT NULL,
    user_id      BIGINT       NOT NULL,
    role         VARCHAR(16)  NOT NULL,
    content      MEDIUMTEXT   NOT NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session (session_id, id),
    INDEX idx_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE chat_message (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    app_id       BIGINT       NOT NULL,
    user_id      BIGINT       NOT NULL,
    role         VARCHAR(16)  NOT NULL,
    content      MEDIUMTEXT   NOT NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_app_user (app_id, user_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE appointment_order (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_name        VARCHAR(64)  NOT NULL,
    phone            VARCHAR(20)  NOT NULL,
    service_type     VARCHAR(64)  NOT NULL,
    appointment_time DATETIME     NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 密码均为 123456（BCrypt）
INSERT INTO user (username, password, nickname, role) VALUES
('admin', '$2a$10$XvxlUd9VbcxhVqfC2HVTyeiw4l0/7imEzD1oKQbCq9FP6nRGEwzR6', '管理员', 'ADMIN'),
('demo',  '$2a$10$XvxlUd9VbcxhVqfC2HVTyeiw4l0/7imEzD1oKQbCq9FP6nRGEwzR6', '演示用户', 'USER');

INSERT INTO app (user_id, name, description, code_type, code_content, is_featured, status) VALUES
(2, '个人主页', 'AI 生成的个人介绍页', 'HTML', '<html><body><h1>Hello AI Platform</h1></body></html>', 1, 'DRAFT');

INSERT INTO appointment_order (user_name, phone, service_type, appointment_time, status) VALUES
('张三', '138****0001', '社区帮扶', '2026-06-28 09:00:00', 'CONFIRMED');
