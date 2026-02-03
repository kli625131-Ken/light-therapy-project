package com.lontri.lighttherapy.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lontri.lighttherapy.entity.SurveyQuestion;

import java.util.List;

public interface SurveyQuestionRepository extends JpaRepository<SurveyQuestion, Long> {
    List<SurveyQuestion> findByTemplateIdOrderByQuestionNoAsc(Long templateId);
    boolean existsByTemplateIdAndQuestionNo(Long templateId, Integer questionNo);
    void deleteByTemplateId(Long templateId);
	int countByTemplateId(Long id);
}

