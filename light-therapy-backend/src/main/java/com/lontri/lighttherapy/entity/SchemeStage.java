package com.lontri.lighttherapy.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "scheme_stage", uniqueConstraints = @UniqueConstraint(name="uk_scheme_stage_no", columnNames = {"scheme_id","stage_no"}))
public class SchemeStage {	
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(name="scheme_id", nullable = false)
	private Long schemeId;
	
	@Column(name="stage_no", nullable = false)
	private Integer stageNo;
	
	// SQL: name VARCHAR(128) NULL
	@Column(name="name", length = 128)
	private String name;
	
	@Column(name="start_day_offset", nullable = false)
	private Integer startDayOffset = 0;
	
	@Column(name="duration_minutes", nullable = false)
	private Integer durationMinutes = 1;
	
	@Column(name="light_intensity")
	private Integer lightIntensity;
	
	@Column(name="light_color_temp")
	private Integer lightColorTemp;
	
	@Column(name="session_count_per_day", nullable = false)
	private Integer sessionCountPerDay = 1;
	
	// SQL: notes VARCHAR(1024) NULL
	@Column(name="notes", length = 1024)
	private String notes;
	
	@Column(name="created_at", nullable = false, updatable = false,
	     insertable = false) // 让 DB DEFAULT CURRENT_TIMESTAMP 生效
	private LocalDateTime createdAt;
	
	@Column(name="updated_at", nullable = false,
	     insertable = false) // 让 DB DEFAULT/ON UPDATE 生效
	private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSchemeId() { return schemeId; }
    public void setSchemeId(Long schemeId) { this.schemeId = schemeId; }    
    public Integer getStartDayOffset() {return startDayOffset;}
	public void setStartDayOffset(Integer startDayOffset) {	this.startDayOffset = startDayOffset;}
	public Integer getLightIntensity() {return lightIntensity;}
	public void setLightIntensity(Integer lightIntensity) {	this.lightIntensity = lightIntensity;}
	public Integer getLightColorTemp() {return lightColorTemp;}
	public void setLightColorTemp(Integer lightColorTemp) {	this.lightColorTemp = lightColorTemp;}
	public Integer getSessionCountPerDay() {return sessionCountPerDay;}
	public void setSessionCountPerDay(Integer sessionCountPerDay) {	this.sessionCountPerDay = sessionCountPerDay;}
	public String getNotes() {return notes;}
	public void setNotes(String notes) {this.notes = notes;}
	public Integer getStageNo() { return stageNo; }
    public void setStageNo(Integer stageNo) { this.stageNo = stageNo; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }    
    public Integer getDurationMinutes() {return durationMinutes;}
	public void setDurationMinutes(Integer durationMinutes) {this.durationMinutes = durationMinutes;}
	public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
