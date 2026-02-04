package com.lontri.lighttherapy.entity;

import javax.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "survey_result", uniqueConstraints = @UniqueConstraint(name = "uk_result_session_template", columnNames = {
		"session_id", "template_id" }))
public class SurveyResult {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "template_id", nullable = false)
	private Long templateId;

	@Column(name = "subject_id", nullable = false)
	private Long subjectId;

	@Column(name = "scheme_id")
	private Long schemeId;

	@Column(name = "session_id", nullable = false)
	private Long sessionId;

	@Column(nullable = false, length = 16)
	private String status; // PENDING / SUBMITTED

	@Column(name = "filled_at", nullable = true)
	private LocalDateTime filledAt; // 提交时写入

	@Column(precision = 10, scale = 2)
	private BigDecimal score;

	@Column(name = "raw_json", columnDefinition = "json")
	private String rawJson; // PENDING: null

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getTemplateId() {
		return templateId;
	}

	public void setTemplateId(Long templateId) {
		this.templateId = templateId;
	}

	public Long getSubjectId() {
		return subjectId;
	}

	public void setSubjectId(Long subjectId) {
		this.subjectId = subjectId;
	}

	public Long getSchemeId() {
		return schemeId;
	}

	public void setSchemeId(Long schemeId) {
		this.schemeId = schemeId;
	}

	public Long getSessionId() {
		return sessionId;
	}

	public void setSessionId(Long sessionId) {
		this.sessionId = sessionId;
	}

	public LocalDateTime getFilledAt() {
		return filledAt;
	}

	public void setFilledAt(LocalDateTime filledAt) {
		this.filledAt = filledAt;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public BigDecimal getScore() {
		return score;
	}

	public void setScore(BigDecimal score) {
		this.score = score;
	}

	public String getRawJson() {
		return rawJson;
	}

	public void setRawJson(String rawJson) {
		this.rawJson = rawJson;
	}

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	public void prePersist() {
		LocalDateTime now = LocalDateTime.now();
		if (createdAt == null)
			createdAt = now;
		if (updatedAt == null)
			updatedAt = now;
	}

	@PreUpdate
	public void preUpdate() {
		updatedAt = LocalDateTime.now();
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
