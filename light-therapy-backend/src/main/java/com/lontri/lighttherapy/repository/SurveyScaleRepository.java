package com.lontri.lighttherapy.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lontri.lighttherapy.entity.SurveyScale;

public interface SurveyScaleRepository extends JpaRepository<SurveyScale, Long> {
    Optional<SurveyScale> findByCode(String code);
}

