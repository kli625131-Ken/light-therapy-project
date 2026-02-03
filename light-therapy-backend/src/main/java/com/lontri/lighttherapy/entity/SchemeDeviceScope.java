package com.lontri.lighttherapy.entity;

import java.time.LocalDateTime;

import javax.persistence.*;

import com.lontri.lighttherapy.enums.DeviceType;
import com.lontri.lighttherapy.enums.SendMode;

@Entity
@Table(name = "scheme_device_scope")
public class SchemeDeviceScope {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="scheme_id", nullable = false)
    private Long schemeId;

    @Column(name="gateway_id", length = 24)
    private String gatewayId;

    @Enumerated(EnumType.STRING)
    @Column(name="device_type", length = 16)
    private DeviceType deviceType;

    @Enumerated(EnumType.STRING)
    @Column(name="send_mode", length = 16)
    private SendMode sendMode;

    @Column(name="created_at", updatable = false)
    private LocalDateTime createdAt;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getSchemeId() {
		return schemeId;
	}

	public void setSchemeId(Long schemeId) {
		this.schemeId = schemeId;
	}

	public String getGatewayId() {
		return gatewayId;
	}

	public void setGatewayId(String gatewayId) {
		this.gatewayId = gatewayId;
	}

	public DeviceType getDeviceType() {
		return deviceType;
	}

	public void setDeviceType(DeviceType deviceType) {
		this.deviceType = deviceType;
	}

	public SendMode getSendMode() {
		return sendMode;
	}

	public void setSendMode(SendMode sendMode) {
		this.sendMode = sendMode;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

    
}
