CREATE DATABASE IF NOT EXISTS ai_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE ai_platform;

-- 用户表
CREATE TABLE IF NOT EXISTS user (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    username     VARCHAR(64)  NOT NULL COMMENT '用户名',
    password     VARCHAR(128) NOT NULL COMMENT 'BCrypt 密码',
    nickname     VARCHAR(64)  DEFAULT NULL COMMENT '昵称',
    avatar       VARCHAR(512) DEFAULT NULL COMMENT '头像 URL',
    role         VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT '角色：USER/ADMIN',
    status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';

-- 应用表
CREATE TABLE IF NOT EXISTS app (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT       NOT NULL COMMENT '创建者 ID',
    name         VARCHAR(128) NOT NULL COMMENT '应用名称',
    description  VARCHAR(512) DEFAULT NULL COMMENT '应用描述',
    code_type    VARCHAR(32)  NOT NULL DEFAULT 'HTML' COMMENT '代码类型：HTML/VUE/MULTI',
    code_content MEDIUMTEXT   DEFAULT NULL COMMENT '生成的代码内容',
    deploy_url   VARCHAR(512) DEFAULT NULL COMMENT '部署访问地址',
    cover_url    VARCHAR(512) DEFAULT NULL COMMENT '封面图 URL',
    is_featured  TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否精选',
    status       VARCHAR(20)  NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/DEPLOYED',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_featured (is_featured, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 生成应用';

-- 平台智能助手会话
CREATE TABLE IF NOT EXISTS assistant_session (
    id           VARCHAR(64)  PRIMARY KEY,
    user_id      BIGINT       NOT NULL COMMENT '用户 ID',
    title        VARCHAR(128) NOT NULL DEFAULT '新对话' COMMENT '会话标题',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_updated (user_id, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能助手会话';

-- 平台智能助手消息
CREATE TABLE IF NOT EXISTS assistant_message (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id   VARCHAR(64)  NOT NULL COMMENT '会话 ID',
    user_id      BIGINT       NOT NULL COMMENT '用户 ID',
    role         VARCHAR(16)  NOT NULL COMMENT '角色：USER/ASSISTANT',
    content      MEDIUMTEXT   NOT NULL COMMENT '消息内容',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session (session_id, id),
    INDEX idx_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能助手消息';

-- 对话历史表
CREATE TABLE IF NOT EXISTS chat_message (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    app_id       BIGINT       NOT NULL COMMENT '应用 ID',
    user_id      BIGINT       NOT NULL COMMENT '用户 ID',
    role         VARCHAR(16)  NOT NULL COMMENT '角色：USER/AI',
    content      MEDIUMTEXT   NOT NULL COMMENT '消息内容',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_app_user (app_id, user_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话历史';

-- 预约订单（保留原有业务）
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
