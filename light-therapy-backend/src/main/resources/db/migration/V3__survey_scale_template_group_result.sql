CREATE TABLE IF NOT EXISTS `survey_scale` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `code` VARCHAR(64) NOT NULL,
  `name` VARCHAR(128) NOT NULL,
  `description` VARCHAR(1024) NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scale_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE `survey_template`
  ADD COLUMN `scale_id` BIGINT NOT NULL AFTER `id`,
  ADD KEY `idx_template_scale` (`scale_id`);

ALTER TABLE `survey_template`
  ADD CONSTRAINT `fk_template_scale`
  FOREIGN KEY (`scale_id`) REFERENCES `survey_scale`(`id`)
  ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `survey_template`
  ADD UNIQUE KEY `uk_scale_ver` (`scale_id`, `version`);


ALTER TABLE `experiment_group`
  ADD COLUMN `survey_template_id` BIGINT NULL,
  ADD KEY `idx_group_survey_template` (`survey_template_id`);

ALTER TABLE `experiment_group`
  ADD CONSTRAINT `fk_group_survey_template`
  FOREIGN KEY (`survey_template_id`) REFERENCES `survey_template`(`id`)
  ON UPDATE CASCADE ON DELETE SET NULL;

-- survey_result: session_id 要 NOT NULL，因此 fk_result_session 必须是 CASCADE
ALTER TABLE `survey_result`
  DROP FOREIGN KEY `fk_result_session`;

ALTER TABLE `survey_result`
  MODIFY COLUMN `template_id` BIGINT NOT NULL,
  MODIFY COLUMN `session_id` BIGINT NOT NULL;

ALTER TABLE `survey_result`
  ADD CONSTRAINT `fk_result_session`
  FOREIGN KEY (`session_id`) REFERENCES `treatment_session` (`id`)
  ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE `survey_result`
  ADD COLUMN `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' AFTER `session_id`;

ALTER TABLE `survey_result`
  MODIFY COLUMN `raw_json` JSON NULL;

ALTER TABLE `survey_result`
  ADD UNIQUE KEY `uk_result_session_template` (`session_id`, `template_id`);
