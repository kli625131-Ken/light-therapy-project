package com.lontri.lighttherapy.repository;

import com.lontri.lighttherapy.entity.SchemeStage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SchemeStageRepository extends JpaRepository<SchemeStage, Long> {
    List<SchemeStage> findBySchemeIdOrderByStageNoAsc(Long schemeId);
    boolean existsBySchemeId(Long schemeId);
	Optional<SchemeStage> findFirstBySchemeIdOrderByStageNoAsc(Long schemeId);
    void deleteBySchemeId(Long schemeId);
}
