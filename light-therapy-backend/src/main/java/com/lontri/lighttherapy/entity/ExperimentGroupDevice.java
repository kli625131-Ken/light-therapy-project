package com.lontri.lighttherapy.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "experiment_group_device",
    uniqueConstraints = @UniqueConstraint(name = "uk_group_device", columnNames = {"group_id", "device_id"}),
    indexes = {
        @Index(name = "idx_egd_group", columnList = "group_id"),
        @Index(name = "idx_egd_device", columnList = "device_id")
    }
)
public class ExperimentGroupDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "device_id", nullable = false)
    private Long deviceId;

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

    public ExperimentGroupDevice setId(Long id) {
        this.id = id;
        return this;
    }

    public Long getGroupId() {
        return groupId;
    }

    public ExperimentGroupDevice setGroupId(Long groupId) {
        this.groupId = groupId;
        return this;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public ExperimentGroupDevice setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public ExperimentGroupDevice setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }
}
