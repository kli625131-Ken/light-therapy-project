-- V2__scheme_stage_duration_to_minutes.sql

-- 1) duration_days -> duration_minutes
ALTER TABLE scheme_stage
  CHANGE COLUMN duration_days duration_minutes INT NOT NULL DEFAULT 1;

CREATE TABLE IF NOT EXISTS scheme_group (
  id BIGINT NOT NULL AUTO_INCREMENT,
  scheme_id BIGINT NOT NULL,
  group_id BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_scheme_group (scheme_id, group_id),
  CONSTRAINT fk_sg_scheme FOREIGN KEY (scheme_id) REFERENCES scheme(id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_sg_group FOREIGN KEY (group_id) REFERENCES experiment_group(id) ON DELETE RESTRICT ON UPDATE CASCADE
);
