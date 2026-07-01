-- 为已有 ai_platform 库增加智能助手历史表（无需重建全库）
USE ai_platform;

CREATE TABLE IF NOT EXISTS assistant_session (
    id           VARCHAR(64)  PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    title        VARCHAR(128) NOT NULL DEFAULT '新对话',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_updated (user_id, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS assistant_message (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id   VARCHAR(64)  NOT NULL,
    user_id      BIGINT       NOT NULL,
    role         VARCHAR(16)  NOT NULL,
    content      MEDIUMTEXT   NOT NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session (session_id, id),
    INDEX idx_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
