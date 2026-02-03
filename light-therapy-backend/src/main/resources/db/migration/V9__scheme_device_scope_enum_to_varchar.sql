-- 把 scheme_device_scope 的 enum 改成 varchar
ALTER TABLE scheme_device_scope
  MODIFY COLUMN device_type VARCHAR(16) NULL;

ALTER TABLE scheme_device_scope
  MODIFY COLUMN send_mode VARCHAR(16) NULL;
