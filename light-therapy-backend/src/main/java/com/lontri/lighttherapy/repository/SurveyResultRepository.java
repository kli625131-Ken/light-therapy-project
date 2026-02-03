package com.lontri.lighttherapy.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lontri.lighttherapy.entity.SurveyResult;

public interface SurveyResultRepository extends JpaRepository<SurveyResult, Long> {
    boolean existsBySessionIdAndTemplateId(Long sessionId, Long templateId);

    List<SurveyResult> findBySessionId(Long sessionId);

    List<SurveyResult> findBySubjectIdOrderByFilledAtDesc(Long subjectId);
}
