package com.lontri.lighttherapy.executor.port;

import com.lontri.lighttherapy.entity.SchemeStage;

public interface StageProvider {
    java.util.List<SchemeStage> getStages(long schemeId);
}