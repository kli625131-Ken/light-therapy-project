package com.lontri.lighttherapy.service;

import com.lontri.lighttherapy.dto.SchemeDtos;
import com.lontri.lighttherapy.dto.SchemeDtos.CreateSchemeWithStagesReq;
import com.lontri.lighttherapy.entity.Scheme;
import com.lontri.lighttherapy.entity.SchemeStage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SchemeService {
    Page<Scheme> list(Long groupId, Pageable pageable);
    Scheme getById(Long id);
    Scheme create(SchemeDtos.CreateSchemeReq req);
    Scheme update(Long id, SchemeDtos.UpdateSchemeReq req);
    Scheme updateWithStages(Long id, SchemeDtos.UpdateSchemeWithStagesReq req);
    void delete(Long id);

    List<SchemeStage> listStages(Long schemeId);
    SchemeStage createStage(Long schemeId, SchemeDtos.CreateStageReq req);
    SchemeStage updateStage(Long id, SchemeDtos.UpdateStageReq req);
    void deleteStage(Long id);    

    Scheme createStageWithStages(CreateSchemeWithStagesReq req);

    Page<Scheme> listGlobalActive(Pageable pageable);
    Page<Scheme> listGroupActive(Long groupId,Pageable pageable);
    Page<Scheme> listAllGroupActive(Pageable pageable);
    Page<Scheme> listRecommended(Long groupId, Pageable pageable);
    Page<Scheme> listAllActive(Pageable pageable);
    Page<SchemeDtos.SchemeWithStagesDTO> listAllActiveWithStages(Pageable pageable);
    
    // 包含阶段信息的方案列表
    Page<SchemeDtos.SchemeWithStagesDTO> listGlobalActiveWithStages(Pageable pageable);
    Page<SchemeDtos.SchemeWithStagesDTO> listGroupActiveWithStages(Long groupId, Pageable pageable);
    Page<SchemeDtos.SchemeWithStagesDTO> listRecommendedWithStages(Long groupId, Pageable pageable);
}
