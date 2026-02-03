-- 指令模板表：按 设备类型 + 功能 唯一
CREATE TABLE IF NOT EXISTS command_template (
  id BIGINT NOT NULL AUTO_INCREMENT,
  device_type VARCHAR(16) NOT NULL COMMENT '设备类型：LONTRI / DALI / 485',
  function_code VARCHAR(16) NOT NULL COMMENT '功能编码：DIM / CCT / POWER 等',
  template VARCHAR(255) NOT NULL COMMENT '指令模板，支持占位符，如 {dim_hex} / {cct_payload}',
  param_schema_json TEXT NULL COMMENT '参数编码规则（JSON，可选）',
  enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  remark VARCHAR(255) NULL COMMENT '备注说明',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_device_type_func (device_type, function_code),
  KEY idx_device_type (device_type),
  KEY idx_function_code (function_code),
  KEY idx_enabled (enabled)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

  INSERT INTO command_template
(device_type, function_code, template, param_schema_json, remark)
VALUES
(
  'LONTRI',
  'DIM',
  'A108FFFF02{dim_hex}',
  '{
    "params": [
      {
        "name": "brightnessPercent",
        "placeholder": "{dim_hex}",
        "transform": "percent_to_dali_0_254",
        "hexLen": 2
      }
    ]
  }',
  'LONTRI 调光：百分比 -> DALI(0~254) -> 1字节HEX'
);

INSERT INTO command_template
(device_type, function_code, template, param_schema_json, remark)
VALUES
(
  'LONTRI',
  'CCT',
  '5A14FFFF0201EEEE{cct_payload}',
  '{
    "params": [
      {
        "name": "cctK",
        "placeholder": "{cct_payload}",
        "transform": "cctK_to_dt8_A3LLC3HH",
        "hexLen": 8
      }
    ]
  }',
  'LONTRI 调色温：K -> DT8(mired) -> A3{low}C3{high}'
);
