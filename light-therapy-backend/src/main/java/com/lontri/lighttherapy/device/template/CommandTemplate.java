package com.lontri.lighttherapy.device.template;

import javax.persistence.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "command_template",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_device_type_func", columnNames = {"device_type", "function_code"})
        },
        indexes = {
                @Index(name = "idx_device_type", columnList = "device_type"),
                @Index(name = "idx_function_code", columnList = "function_code"),
                @Index(name = "idx_enabled", columnList = "enabled")
        }
)
public class CommandTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // LONTRI / DALI / 485
    @Column(name = "device_type", length = 16, nullable = false)
    private String deviceType;

    // DIM / CCT / POWER / ...
    @Column(name = "function_code", length = 16, nullable = false)
    private String functionCode;

    // e.g. A108FFFF02{dim_hex}
    @Column(name = "template", length = 255, nullable = false)
    private String template;

    // JSON string e.g. {"params":[{"name":"brightnessPercent"...}]}
    @Lob
    @Column(name = "param_schema_json")
    private String paramSchemaJson;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "remark", length = 255)
    private String remark;

    @CreationTimestamp
    @Column(name="created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name="updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (enabled == null) enabled = true;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getDeviceType() {
		return deviceType;
	}

	public void setDeviceType(String deviceType) {
		this.deviceType = deviceType;
	}

	public String getFunctionCode() {
		return functionCode;
	}

	public void setFunctionCode(String functionCode) {
		this.functionCode = functionCode;
	}

	public String getTemplate() {
		return template;
	}

	public void setTemplate(String template) {
		this.template = template;
	}

	public String getParamSchemaJson() {
		return paramSchemaJson;
	}

	public void setParamSchemaJson(String paramSchemaJson) {
		this.paramSchemaJson = paramSchemaJson;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
    
}