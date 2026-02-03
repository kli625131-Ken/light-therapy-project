-- 3) 删除旧唯一约束：仅当存在时才删
SET @idx := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'treatment_session_device'
    AND index_name = 'uk_session_device'
);

SET @sql := IF(@idx > 0,
  'ALTER TABLE treatment_session_device DROP INDEX uk_session_device',
  'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
