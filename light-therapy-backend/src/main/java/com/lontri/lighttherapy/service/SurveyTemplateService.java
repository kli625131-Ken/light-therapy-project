package com.lontri.lighttherapy.service;

import java.util.List;

import com.lontri.lighttherapy.dto.SurveyDtos;
import com.lontri.lighttherapy.dto.SurveyDtos.UpdateTemplateReq;
import com.lontri.lighttherapy.entity.SurveyTemplate;

public interface SurveyTemplateService {

    List<SurveyDtos.TemplateSummary> listTemplates();

    SurveyDtos.TemplateDetail getTemplate(Long id);
    
    SurveyDtos.TemplateDetail getDefaultTemplate();

    SurveyTemplate saveTemplate(SurveyDtos.SaveTemplateReq req);

	SurveyTemplate updateTemplate(Long id, UpdateTemplateReq req);
    
    void deleteTemplate(Long id);
}

