package com.lontri.lighttherapy.entity;

//SurveyQuestion.java
import javax.persistence.*;

import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "survey_question",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_survey_question_no",
        columnNames = {"template_id", "question_no"}
    )
)
public class SurveyQuestion {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(name = "question_no", nullable = false)
    private Integer questionNo;

    @Column(length = 32, nullable = false)
    private String type; // RANGE / SELECT / TEXT

    @Column(length = 512, nullable = false)
    private String title;

    @Column(name = "options_json", columnDefinition = "json")
    private String optionsJson;

    @Column(nullable = false)
    private Boolean required = true;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

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

	public Integer getQuestionNo() {
		return questionNo;
	}

	public void setQuestionNo(Integer questionNo) {
		this.questionNo = questionNo;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getOptionsJson() {
		return optionsJson;
	}

	public void setOptionsJson(String optionsJson) {
		this.optionsJson = optionsJson;
	}

	public Boolean getRequired() {
		return required;
	}

	public void setRequired(Boolean required) {
		this.required = required;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}


}
