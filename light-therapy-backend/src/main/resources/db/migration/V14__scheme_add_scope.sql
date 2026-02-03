-- Vx__scheme_add_scope.sql

ALTER TABLE scheme
  ADD COLUMN scope VARCHAR(16) NOT NULL DEFAULT 'GROUP' AFTER group_id;

-- 将现有数据修正：group_id 为 NULL 的都认为是 GLOBAL（如果历史上没有这种数据，也没关系）
UPDATE scheme
SET scope = 'GLOBAL'
WHERE group_id IS NULL;

-- 为 scope 增加索引（常用过滤字段）
CREATE INDEX idx_scheme_scope ON scheme(scope);

-- （可选）如果你们 MySQL 开启了 CHECK 并且确实生效，可加：
--ALTER TABLE scheme
--  ADD CONSTRAINT chk_scheme_scope_group
--  CHECK (
--    (scope = 'GLOBAL' AND group_id IS NULL)
--    OR
--    (scope = 'GROUP' AND group_id IS NOT NULL)
--  );
