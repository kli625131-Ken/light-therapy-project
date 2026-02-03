CREATE TABLE `experiment_group_device` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `group_id` bigint NOT NULL,
  `device_id` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_device` (`group_id`, `device_id`),
  KEY `idx_egd_group` (`group_id`),
  KEY `idx_egd_device` (`device_id`),
  CONSTRAINT `fk_egd_group` FOREIGN KEY (`group_id`) REFERENCES `experiment_group` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_egd_device` FOREIGN KEY (`device_id`) REFERENCES `device` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO experiment_group_device (group_id, device_id) VALUES (2,2);

-- ============================================
-- Vxx__tsd_add_device_id.sql
-- Add device_id to treatment_session_device
-- Backfill by joining device_sn -> device.id
-- Upgrade unique constraint to (session_id, device_id)
-- ============================================

-- 1) 添加 device_id（先允许 NULL，方便回填）
ALTER TABLE treatment_session_device
  ADD COLUMN device_id BIGINT NULL AFTER session_id;

-- 2) 回填 device_id（按 device_sn 关联 device 表）
UPDATE treatment_session_device tsd
JOIN device d ON d.device_sn = tsd.device_sn
SET tsd.device_id = d.id
WHERE tsd.device_id IS NULL;

-- 3) 检查是否存在回填失败的记录（必须人工确认处理）
--    如果返回有记录：说明 tsd.device_sn 在 device 表找不到对应记录
SELECT tsd.*
FROM treatment_session_device tsd
LEFT JOIN device d ON d.device_sn = tsd.device_sn
WHERE tsd.device_id IS NULL;

-- ⚠️ 如果上面 SELECT 有结果，你需要先处理（补 device 或删除脏数据），否则下一步 NOT NULL 会失败。

-- 4) 把 device_id 改为 NOT NULL
ALTER TABLE treatment_session_device
  MODIFY COLUMN device_id BIGINT NOT NULL;

-- 5) 添加索引（按 device_id 查/关联会更快）
ALTER TABLE treatment_session_device
  ADD KEY idx_tsd_device (device_id);

-- 6) 添加外键：tsd.device_id -> device.id
ALTER TABLE treatment_session_device
  ADD CONSTRAINT fk_tsd_device
    FOREIGN KEY (device_id)
    REFERENCES device(id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE;

-- 7) 升级唯一约束：从 (session_id, device_sn) 改成 (session_id, device_id)
--    先删除旧唯一键，再新增新唯一键
ALTER TABLE treatment_session_device
  DROP INDEX uk_session_device;

ALTER TABLE treatment_session_device
  ADD UNIQUE KEY uk_session_device (session_id, device_id);

-- 8) （可选）保留一个非唯一索引 device_sn（你原来已经有）
-- 你现在已有：KEY idx_tsd_device_sn (device_sn)  无需改

