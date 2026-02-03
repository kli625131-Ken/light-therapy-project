package com.lontri.lighttherapy.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "treatment_session_device",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_session_device",
        columnNames = {"session_id", "device_id"}
    ),
    indexes = {
        @Index(name = "idx_tsd_session", columnList = "session_id"),
        @Index(name = "idx_tsd_device", columnList = "device_id"),
        @Index(name = "idx_tsd_device_sn", columnList = "device_sn")
    }
)
public class TreatmentSessionDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    @Column(name = "device_sn", nullable = false, length = 64)
    private String deviceSn;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // ===== getters / setters =====

    public Long getId() {
        return id;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public TreatmentSessionDevice setSessionId(Long sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public TreatmentSessionDevice setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
        return this;
    }

    public String getDeviceSn() {
        return deviceSn;
    }

    public TreatmentSessionDevice setDeviceSn(String deviceSn) {
        this.deviceSn = deviceSn;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
