-- Enforce only one RUNNING session per subject (MySQL 8 functional index)
CREATE UNIQUE INDEX uk_subject_running
ON treatment_session ((CASE WHEN status = 'RUNNING' THEN subject_id ELSE NULL END));

CREATE TABLE IF NOT EXISTS device (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  device_sn VARCHAR(64) NOT NULL UNIQUE,
  send_mode VARCHAR(16) NOT NULL /* UDP or TCP */,
  udp_ip VARCHAR(64) NULL,
  udp_port INT NULL,
  tcp_server_ip VARCHAR(64) NULL,
  tcp_server_port INT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
