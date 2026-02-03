-- V15__remove_scheme_group_unique_constraint.sql

-- 移除scheme_group表的uk_scheme_group唯一约束
ALTER TABLE scheme_group
DROP INDEX uk_scheme_group;