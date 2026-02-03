ALTER TABLE device
  ADD COLUMN gateway_id BIGINT NULL;

CREATE INDEX idx_device_gateway_id ON device(gateway_id);
