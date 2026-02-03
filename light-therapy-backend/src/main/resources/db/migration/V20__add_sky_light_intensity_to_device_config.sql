-- Add sky light intensity field for 485 device type
ALTER TABLE `scheme_stage_device_config`
ADD COLUMN `sky_light_intensity` INT(11) DEFAULT NULL COMMENT '天空光亮度百分比，仅485设备使用';