-- MySQL数据库创建脚本
-- 基于MySQL数据库表设计文档

-- 1. 创建数据库
CREATE DATABASE IF NOT EXISTS light_therapy 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

-- 2. 使用数据库
USE light_therapy;

-- 3. 创建用户表（user）
CREATE TABLE IF NOT EXISTS `user` (
    `id` VARCHAR(50) PRIMARY KEY,
    `password` VARCHAR(255) NOT NULL,
    `role` ENUM('subject', 'researcher') NOT NULL,
    `gender` ENUM('男', '女') NULL,
    `age` INT(3) NULL,
    `group_id` INT(11) NULL,
    `researcher_name` VARCHAR(100) NULL,
    `contact_info` VARCHAR(200) NULL,
    `enrollment_date` DATE NULL,
    `diagnosis_result` VARCHAR(200) NULL,
    `last_active` DATETIME NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_role (`role`),
    INDEX idx_group_id (`group_id`),
    INDEX idx_created_at (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. 创建分组表（`group`）
CREATE TABLE IF NOT EXISTS `group` (
    `id` INT(11) PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL UNIQUE,
    `description` TEXT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_name (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. 创建治疗方案表（scheme）
CREATE TABLE IF NOT EXISTS `scheme` (
    `id` INT(11) PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(200) NOT NULL,
    `description` TEXT NULL,
    `total_duration` INT(11) NOT NULL,
    `is_custom` TINYINT(1) NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_is_custom (`is_custom`),
    INDEX idx_created_at (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. 创建方案阶段表（scheme_stage）
CREATE TABLE IF NOT EXISTS `scheme_stage` (
    `id` INT(11) PRIMARY KEY AUTO_INCREMENT,
    `scheme_id` INT(11) NOT NULL,
    `stage_order` INT(11) NOT NULL,
    `brightness` INT(11) NOT NULL,
    `temp` INT(11) NOT NULL,
    `duration` INT(11) NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_scheme_id (`scheme_id`),
    INDEX idx_scheme_order (`scheme_id`, `stage_order`),
    FOREIGN KEY (`scheme_id`) REFERENCES `scheme`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. 创建治疗记录表（treatment_log）
CREATE TABLE IF NOT EXISTS `treatment_log` (
    `id` BIGINT(20) PRIMARY KEY AUTO_INCREMENT,
    `user_id` VARCHAR(50) NOT NULL,
    `scheme_id` INT(11) NULL,
    `scheme_name` VARCHAR(200) NOT NULL,
    `date` DATE NOT NULL,
    `start_time` TIME NOT NULL,
    `end_time` TIME NOT NULL,
    `duration_actual` INT(11) NOT NULL,
    `status` ENUM('自动完成', '手动停止') NOT NULL,
    `manual_brightness` INT(11) NULL,
    `manual_temp` INT(11) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (`user_id`),
    INDEX idx_scheme_id (`scheme_id`),
    INDEX idx_date (`date`),
    INDEX idx_created_at (`created_at`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`scheme_id`) REFERENCES `scheme`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. 创建问卷模板表（survey_template）
CREATE TABLE IF NOT EXISTS `survey_template` (
    `id` INT(11) PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(200) NOT NULL,
    `description` TEXT NULL,
    `simplified_mode` TINYINT(1) NOT NULL DEFAULT 0,
    `is_active` TINYINT(1) NOT NULL DEFAULT 1,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_is_active (`is_active`),
    INDEX idx_created_at (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. 创建问卷问题表（survey_question）
CREATE TABLE IF NOT EXISTS `survey_question` (
    `id` INT(11) PRIMARY KEY AUTO_INCREMENT,
    `template_id` INT(11) NOT NULL,
    `question_order` INT(11) NOT NULL,
    `type` ENUM('range', 'select', 'text') NOT NULL,
    `label` TEXT NOT NULL,
    `min_value` INT(11) NULL,
    `max_value` INT(11) NULL,
    `options` TEXT NULL,
    `required` TINYINT(1) NOT NULL DEFAULT 1,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_template_id (`template_id`),
    INDEX idx_template_order (`template_id`, `question_order`),
    FOREIGN KEY (`template_id`) REFERENCES `survey_template`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. 创建问卷结果表（survey_result）
CREATE TABLE IF NOT EXISTS `survey_result` (
    `id` BIGINT(20) PRIMARY KEY AUTO_INCREMENT,
    `user_id` VARCHAR(50) NOT NULL,
    `treatment_log_id` BIGINT(20) NOT NULL,
    `template_id` INT(11) NOT NULL,
    `answers` TEXT NOT NULL,
    `total_score` INT(11) NULL,
    `date` DATE NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (`user_id`),
    INDEX idx_treatment_log_id (`treatment_log_id`),
    INDEX idx_template_id (`template_id`),
    INDEX idx_date (`date`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`treatment_log_id`) REFERENCES `treatment_log`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`template_id`) REFERENCES `survey_template`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. 创建数据备份表（data_backup）
CREATE TABLE IF NOT EXISTS `data_backup` (
    `id` INT(11) PRIMARY KEY AUTO_INCREMENT,
    `backup_filename` VARCHAR(255) NOT NULL,
    `backup_path` VARCHAR(500) NOT NULL,
    `backup_size` BIGINT(20) NOT NULL,
    `backup_type` ENUM('full', 'incremental') NOT NULL,
    `status` ENUM('success', 'failed') NOT NULL,
    `created_by` VARCHAR(50) NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_created_by (`created_by`),
    INDEX idx_created_at (`created_at`),
    INDEX idx_status (`status`),
    FOREIGN KEY (`created_by`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12. 验证数据库和表是否创建成功
SHOW DATABASES LIKE 'light_therapy';
SHOW TABLES IN light_therapy;

-- 13. 显示表结构（可选，用于验证）
-- DESC `user`;
-- DESC `group`;
-- DESC `scheme`;
-- DESC `scheme_stage`;
-- DESC `treatment_log`;
-- DESC `survey_template`;
-- DESC `survey_question`;
-- DESC `survey_result`;
-- DESC `data_backup`;
