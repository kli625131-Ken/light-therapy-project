-- =========================================================
-- Lontri Light Therapy - MySQL 8.x Initialization Script
-- Schema: light_therapy (utf8mb4)
-- Notes:
-- 1) Avoid reserved words: use `users`, `experiment_group` (NOT `user`, `group`)
-- 2) Designed to match backend API planning:
--    /api/auth/login, /api/groups, /api/schemes, /api/surveys, /api/sessions
-- =========================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 1) Create database
--  DEFAULT CHARACTER SET utf8mb4
--  DEFAULT COLLATE utf8mb4_0900_ai_ci;


-- 2) Users (auth)
CREATE TABLE IF NOT EXISTS `users` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(64) NOT NULL,
  `password_hash` VARCHAR(255) NOT NULL,
  `role` VARCHAR(32) NOT NULL,           -- ADMIN / SUBJECT
  `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE / DISABLED
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3) Experiment groups
CREATE TABLE IF NOT EXISTS `experiment_group` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(128) NOT NULL,
  `description` VARCHAR(512) NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4) Subjects
CREATE TABLE IF NOT EXISTS `subjects` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `subject_code` VARCHAR(64) NOT NULL,
  `display_name` VARCHAR(128) NULL,
  `gender` VARCHAR(16) NULL,       -- M/F/OTHER
  `age` INT NULL,
  `group_id` BIGINT NULL,
  `enrollment_date` DATE NULL,
  `diagnosis_result` VARCHAR(255) NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE / PAUSED / DROPPED
  `researcher_name` VARCHAR(100) NULL,
  `contact_info` VARCHAR(255) NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_subject_code` (`subject_code`),
  UNIQUE KEY `uk_subject_user` (`user_id`),
  KEY `idx_subject_group` (`group_id`),
  KEY `idx_subject_user` (`user_id`),
    CONSTRAINT `fk_subject_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON UPDATE CASCADE ON DELETE CASCADE,
CONSTRAINT `fk_subject_group` FOREIGN KEY (`group_id`) REFERENCES `experiment_group` (`id`)
    ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5) Schemes (lighting protocol)
CREATE TABLE IF NOT EXISTS `scheme` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `group_id` BIGINT NULL,                 -- optional: scheme bound to a group
  `name` VARCHAR(128) NOT NULL,
  `description` VARCHAR(1024) NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE / ARCHIVED
  `created_by` BIGINT NULL,               -- users.id
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_scheme_group` (`group_id`),
  KEY `idx_scheme_creator` (`created_by`),
  CONSTRAINT `fk_scheme_group` FOREIGN KEY (`group_id`) REFERENCES `experiment_group` (`id`)
    ON UPDATE CASCADE ON DELETE SET NULL,
  CONSTRAINT `fk_scheme_creator` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`)
    ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6) Scheme stages
CREATE TABLE IF NOT EXISTS `scheme_stage` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `scheme_id` BIGINT NOT NULL,
  `stage_no` INT NOT NULL,                -- 1..N
  `name` VARCHAR(128) NULL,
  `start_day_offset` INT NOT NULL DEFAULT 0,  -- day offset from scheme start
  `duration_days` INT NOT NULL DEFAULT 1,
  `light_intensity` INT NULL,             -- e.g. lux
  `light_color_temp` INT NULL,            -- e.g. kelvin
  `session_count_per_day` INT NOT NULL DEFAULT 1,
  `notes` VARCHAR(1024) NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scheme_stage_no` (`scheme_id`, `stage_no`),
  CONSTRAINT `fk_stage_scheme` FOREIGN KEY (`scheme_id`) REFERENCES `scheme` (`id`)
    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7) Surveys (questionnaire templates)
CREATE TABLE IF NOT EXISTS `survey_template` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `code` VARCHAR(64) NOT NULL,
  `name` VARCHAR(128) NOT NULL,
  `version` INT NOT NULL DEFAULT 1,
  `description` VARCHAR(1024) NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE / ARCHIVED
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_survey_code_ver` (`code`, `version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `survey_question` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `template_id` BIGINT NOT NULL,
  `question_no` INT NOT NULL,
  `type` VARCHAR(32) NOT NULL,            -- SINGLE/MULTI/SCALE/TEXT/NUMBER
  `title` VARCHAR(512) NOT NULL,
  `options_json` JSON NULL,               -- for single/multi/scale
  `required` TINYINT(1) NOT NULL DEFAULT 1,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_survey_question_no` (`template_id`, `question_no`),
  CONSTRAINT `fk_question_template` FOREIGN KEY (`template_id`) REFERENCES `survey_template` (`id`)
    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8) Treatment sessions
CREATE TABLE IF NOT EXISTS `treatment_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `subject_id` BIGINT NOT NULL,
  `scheme_id` BIGINT NOT NULL,
  `stage_id` BIGINT NULL,
  `planned_start_time` DATETIME NULL,
  `start_time` DATETIME NULL,
  `end_time` DATETIME NULL,
  `device_sn` VARCHAR(64) NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'PLANNED', -- PLANNED/RUNNING/DONE/FAILED/CANCELLED
  `metrics_json` JSON NULL,                 -- device metrics, duration, etc.
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_session_subject` (`subject_id`),
  KEY `idx_session_scheme` (`scheme_id`),
  KEY `idx_session_stage` (`stage_id`),
  CONSTRAINT `fk_session_subject` FOREIGN KEY (`subject_id`) REFERENCES `subjects` (`id`)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT `fk_session_scheme` FOREIGN KEY (`scheme_id`) REFERENCES `scheme` (`id`)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT `fk_session_stage` FOREIGN KEY (`stage_id`) REFERENCES `scheme_stage` (`id`)
    ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `treatment_event` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `session_id` BIGINT NOT NULL,
  `event_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `event_type` VARCHAR(32) NOT NULL,        -- START/STOP/DEVICE_MSG/ERROR
  `message` VARCHAR(1024) NULL,
  `detail_json` JSON NULL,
  PRIMARY KEY (`id`),
  KEY `idx_event_session` (`session_id`),
  CONSTRAINT `fk_event_session` FOREIGN KEY (`session_id`) REFERENCES `treatment_session` (`id`)
    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 9) Survey results (filled by subject; optionally tied to session)
CREATE TABLE IF NOT EXISTS `survey_result` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `template_id` BIGINT NOT NULL,
  `subject_id` BIGINT NOT NULL,
  `scheme_id` BIGINT NULL,
  `session_id` BIGINT NULL,
  `filled_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `score` DECIMAL(10,2) NULL,
  `raw_json` JSON NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_result_template` (`template_id`),
  KEY `idx_result_subject` (`subject_id`),
  KEY `idx_result_session` (`session_id`),
  CONSTRAINT `fk_result_template` FOREIGN KEY (`template_id`) REFERENCES `survey_template` (`id`)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT `fk_result_subject` FOREIGN KEY (`subject_id`) REFERENCES `subjects` (`id`)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT `fk_result_scheme` FOREIGN KEY (`scheme_id`) REFERENCES `scheme` (`id`)
    ON UPDATE CASCADE ON DELETE SET NULL,
  CONSTRAINT `fk_result_session` FOREIGN KEY (`session_id`) REFERENCES `treatment_session` (`id`)
    ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 10) Audit log
CREATE TABLE IF NOT EXISTS `audit_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NULL,
  `action` VARCHAR(64) NOT NULL,            -- LOGIN/CREATE/UPDATE/DELETE/EXPORT/IMPORT
  `entity` VARCHAR(64) NULL,
  `entity_id` VARCHAR(64) NULL,
  `detail_json` JSON NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_audit_user` (`user_id`),
  CONSTRAINT `fk_audit_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 11) Backup records
CREATE TABLE IF NOT EXISTS `backup_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `backup_type` VARCHAR(32) NOT NULL,       -- FULL/INCREMENTAL/EXPORT
  `file_path` VARCHAR(512) NOT NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'CREATED', -- CREATED/DONE/FAILED
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 12) Device calibration rules (optional)
CREATE TABLE IF NOT EXISTS `calibration_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `device_model` VARCHAR(64) NOT NULL,
  `param` VARCHAR(64) NOT NULL,
  `min_value` DECIMAL(12,4) NULL,
  `max_value` DECIMAL(12,4) NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cal_rule` (`device_model`, `param`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 13) Seed admin user (password: admin123)
-- NOTE: replace with BCrypt in production; this seed assumes backend supports BCrypt.
-- BCrypt for 'admin123' (cost=10) example:
-- $2a$10$Z1q2aXJbYV8o2Tt/5m6xveE5lYc9x6o7uJ9EoQO8fJYxPqP2eX0fS
INSERT INTO `users` (`username`, `password_hash`, `role`, `status`)
SELECT 'admin', '$2a$10$Z1q2aXJbYV8o2Tt/5m6xveE5lYc9x6o7uJ9EoQO8fJYxPqP2eX0fS', 'ADMIN', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM `users` WHERE `username`='admin');

SET FOREIGN_KEY_CHECKS = 1;