package com.lontri.lighttherapy.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "scheme_stage_device_config", uniqueConstraints = @UniqueConstraint(name="uk_stage_device", columnNames = {"stage_id","device_id"}))
public class SchemeStageDeviceConfig {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name="stage_id", nullable = false)
    private Long stageId;
    
    @Column(name="device_id", nullable = false)
    private Long deviceId;
    
    @Column(name="device_sn", nullable = false, length = 64)
    private String deviceSn;
    
    @Column(name="light_intensity")
    private Integer lightIntensity;
    
    @Column(name="light_color_temp")
    private Integer lightColorTemp;
    
    @Column(name="sky_light_intensity")
    private Integer skyLightIntensity;
    
    @Column(name="created_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdAt;
    
    @Column(name="updated_at", nullable = false, insertable = false)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStageId() {
        return stageId;
    }

    public void setStageId(Long stageId) {
        this.stageId = stageId;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceSn() {
        return deviceSn;
    }

    public void setDeviceSn(String deviceSn) {
        this.deviceSn = deviceSn;
    }

    public Integer getLightIntensity() {
        return lightIntensity;
    }

    public void setLightIntensity(Integer lightIntensity) {
        this.lightIntensity = lightIntensity;
    }

    public Integer getLightColorTemp() {
        return lightColorTemp;
    }

    public void setLightColorTemp(Integer lightColorTemp) {
        this.lightColorTemp = lightColorTemp;
    }
    
    public Integer getSkyLightIntensity() {
        return skyLightIntensity;
    }
    
    public void setSkyLightIntensity(Integer skyLightIntensity) {
        this.skyLightIntensity = skyLightIntensity;
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