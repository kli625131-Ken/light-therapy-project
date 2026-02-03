package com.lontri.lighttherapy.entity;

//SurveyTemplate.java
import javax.persistence.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "survey_template",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_scale_ver",
        columnNames = {"scale_id", "version"}
    )
)
public class SurveyTemplate {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scale_id", nullable = false)
    private Long scaleId;

    @Column(length = 64, nullable = false)
    private String code;

    @Column(length = 128, nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer version;

    @Column(length = 1024)
    private String description;

    @Column(length = 16, nullable = false)
    private String status = "ACTIVE";

    @CreationTimestamp
    @Column(name="created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getScaleId() {
		return scaleId;
	}

	public void setScaleId(Long scaleId) {
		this.scaleId = scaleId;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
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

