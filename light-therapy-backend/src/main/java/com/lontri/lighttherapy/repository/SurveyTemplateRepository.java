package com.lontri.lighttherapy.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.lontri.lighttherapy.entity.SurveyTemplate;

import java.util.List;
import java.util.Optional;

public interface SurveyTemplateRepository extends JpaRepository<SurveyTemplate, Long> {
    boolean existsByCodeAndVersion(String code, Integer version);
    Page<SurveyTemplate> findByStatus(String status, Pageable pageable);
    Page<SurveyTemplate> findByCode(String code, Pageable pageable);
    Optional<SurveyTemplate> findByCodeAndVersion(String code, Integer version);
    List<SurveyTemplate> findByStatusOrderByUpdatedAtDesc(String status);
    boolean existsByScaleIdAndVersion(Long scaleId, Integer version);
}
