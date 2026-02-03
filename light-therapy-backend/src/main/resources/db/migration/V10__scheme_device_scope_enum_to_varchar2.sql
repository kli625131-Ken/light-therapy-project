-- 把 scheme_device_scope 的 enum 改成 varchar
ALTER TABLE scheme_device_scope
  MODIFY COLUMN gateway_id VARCHAR(24) NULL;
