package com.lontri.lighttherapy.entity;
import java.time.LocalDateTime;
import javax.persistence.*;

import com.lontri.lighttherapy.enums.TreatmentSessionStatus;

@Entity
@Table(
    name = "treatment_session",
    indexes = {
        @Index(name = "idx_session_subject", columnList = "subject_id"),
        @Index(name = "idx_session_scheme", columnList = "scheme_id"),
        @Index(name = "idx_session_stage", columnList = "stage_id")
    }
)
public class TreatmentSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(name = "scheme_id", nullable = false)
    private Long schemeId;

    @Column(name = "stage_id")
    private Long stageId;

    @Column(name = "planned_start_time")
    private LocalDateTime plannedStartTime;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    /**
     * 已废弃：多设备后请使用 treatment_session_device
     * 这里保留仅用于兼容历史数据/迁移
     */
    @Deprecated
    @Column(name = "device_sn", length = 64)
    private String deviceSn;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private TreatmentSessionStatus status;

    /**
     * metrics_json: json 类型字段
     * 不引入额外依赖的情况下，最稳是用 String 接住
     */
    @Column(name = "metrics_json", columnDefinition = "json")
    private String metricsJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (status == null) status = TreatmentSessionStatus.PLANNED; // 对齐 DB 默认
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ===== getters/setters =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }

    public Long getSchemeId() { return schemeId; }
    public void setSchemeId(Long schemeId) { this.schemeId = schemeId; }

    public Long getStageId() { return stageId; }
    public void setStageId(Long stageId) { this.stageId = stageId; }

    public LocalDateTime getPlannedStartTime() { return plannedStartTime; }
    public void setPlannedStartTime(LocalDateTime plannedStartTime) { this.plannedStartTime = plannedStartTime; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    /** @deprecated */
    @Deprecated
    public String getDeviceSn() { return deviceSn; }
    /** @deprecated */
    @Deprecated
    public void setDeviceSn(String deviceSn) { this.deviceSn = deviceSn; }

    public TreatmentSessionStatus getStatus() { return status; }
    public void setStatus(TreatmentSessionStatus status) { this.status = status; }

    public String getMetricsJson() { return metricsJson; }
    public void setMetricsJson(String metricsJson) { this.metricsJson = metricsJson; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
