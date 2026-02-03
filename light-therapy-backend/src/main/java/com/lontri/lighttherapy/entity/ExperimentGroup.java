package com.lontri.lighttherapy.entity;

import javax.persistence.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "experiment_group")
public class ExperimentGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 128, unique = true)
    private String name;

    @Column(length = 512)
    private String description;

    @Column(nullable = false, length = 16)
    private String status; // ACTIVE / DISABLED
    
    @CreationTimestamp
    @Column(name="created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name="updated_at")
    private LocalDateTime updatedAt;
    
    // ken260129-修改内容：添加与多个量表的多对多关联关系
    @ManyToMany
    @JoinTable(
        name = "experiment_group_survey_templates",
        joinColumns = @JoinColumn(name = "experiment_group_id"),
        inverseJoinColumns = @JoinColumn(name = "survey_template_id")
    )
    private List<SurveyTemplate> surveyTemplates;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<SurveyTemplate> getSurveyTemplates() { return surveyTemplates; }
    public void setSurveyTemplates(List<SurveyTemplate> surveyTemplates) { this.surveyTemplates = surveyTemplates; }
	
}
