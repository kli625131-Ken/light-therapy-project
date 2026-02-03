package com.lontri.lighttherapy.infra.stage;

import org.springframework.stereotype.Component;

import com.lontri.lighttherapy.entity.SchemeStage;
import com.lontri.lighttherapy.executor.port.StageProvider;
import com.lontri.lighttherapy.repository.SchemeStageRepository;

@Component
public class JpaStageProvider implements StageProvider {
    private final SchemeStageRepository stageRepo;

    public JpaStageProvider(SchemeStageRepository stageRepo) {
        this.stageRepo = stageRepo;
    }

    @Override
    public java.util.List<SchemeStage> getStages(long schemeId) {
        return stageRepo.findBySchemeIdOrderByStageNoAsc(schemeId);
    }
}
