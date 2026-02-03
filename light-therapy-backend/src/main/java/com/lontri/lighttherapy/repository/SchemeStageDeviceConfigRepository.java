package com.lontri.lighttherapy.repository;

import com.lontri.lighttherapy.entity.SchemeStageDeviceConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SchemeStageDeviceConfigRepository extends JpaRepository<SchemeStageDeviceConfig, Long> {
    List<SchemeStageDeviceConfig> findByStageId(Long stageId);
    List<SchemeStageDeviceConfig> findByStageIdIn(List<Long> stageIds);
    void deleteByStageId(Long stageId);
    void deleteByStageIdIn(List<Long> stageIds);
}