-- 添加设备名称字段到device表
ALTER TABLE `device`
ADD COLUMN `device_name` VARCHAR(128) DEFAULT NULL COMMENT '设备名称';