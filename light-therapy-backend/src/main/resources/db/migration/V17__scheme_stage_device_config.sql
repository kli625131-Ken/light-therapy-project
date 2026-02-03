-- Create table for scheme stage device config
CREATE TABLE IF NOT EXISTS `scheme_stage_device_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `stage_id` bigint(20) NOT NULL,
  `device_id` bigint(20) NOT NULL,
  `device_sn` varchar(64) NOT NULL,
  `light_intensity` int(11) DEFAULT NULL,
  `light_color_temp` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stage_device` (`stage_id`,`device_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;