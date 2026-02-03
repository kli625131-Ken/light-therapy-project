-- 1) send_mode 改为 ENUM
ALTER TABLE device
  MODIFY COLUMN send_mode ENUM('UDP','TCP') NOT NULL;

-- 2) gateway_id 从 bigint 改为 char(24)
-- 注意：如果当前 gateway_id 里已经有数值数据，会被转成字符串（例如 123 -> '123'）
ALTER TABLE device
  MODIFY COLUMN gateway_id VARCHAR(24) NULL;

-- 3) 新增 device_type（枚举：LONTRI / DALI / 485）
ALTER TABLE device
  ADD COLUMN device_type ENUM('LONTRI','DALI','485') NOT NULL DEFAULT 'LONTRI';

-- 4) 联合唯一：gateway_id + device_sn
-- 先删除旧的 device_sn 唯一约束（否则会限制全局唯一，与你的联合唯一冲突）

-- 5) gateway_id 普通索引可保留（联合唯一已包含 gateway_id 前缀，一般可删掉减少冗余）
-- 如果你希望保持简单就删掉：

-- 6) 如果你经常按 device_sn 查询（getBySn），建议保留一个普通索引（不是唯一）
CREATE INDEX idx_device_sn ON device(device_sn);

CREATE TABLE scheme_device_scope (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  scheme_id BIGINT NOT NULL,

  gateway_id CHAR(24) NULL,
  device_type ENUM('LONTRI','DALI','485') NULL,
  send_mode ENUM('UDP','TCP') NULL,

  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

  KEY idx_sds_scheme (scheme_id),
  KEY idx_sds_gateway (gateway_id),

  CONSTRAINT fk_sds_scheme
    FOREIGN KEY (scheme_id)
    REFERENCES scheme(id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE treatment_session_device (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_id BIGINT NOT NULL,
  device_sn VARCHAR(64) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

  UNIQUE KEY uk_session_device (session_id, device_sn),
  KEY idx_tsd_device_sn (device_sn),

  CONSTRAINT fk_tsd_session
    FOREIGN KEY (session_id)
    REFERENCES treatment_session(id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
