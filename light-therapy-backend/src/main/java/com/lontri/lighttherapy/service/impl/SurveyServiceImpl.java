package com.lontri.lighttherapy.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lontri.lighttherapy.common.BizException;
import com.lontri.lighttherapy.dto.SurveyDtos;
import com.lontri.lighttherapy.dto.SurveyDtos.QuestionReq;
import com.lontri.lighttherapy.entity.SurveyQuestion;
import com.lontri.lighttherapy.entity.SurveyScale;
import com.lontri.lighttherapy.entity.SurveyTemplate;
import com.lontri.lighttherapy.repository.SurveyQuestionRepository;
import com.lontri.lighttherapy.repository.SurveyScaleRepository;
import com.lontri.lighttherapy.repository.SurveyTemplateRepository;
import com.lontri.lighttherapy.service.SurveyTemplateService;

@Service
public class SurveyServiceImpl implements SurveyTemplateService {

    private static final Logger log = LoggerFactory.getLogger(SurveyServiceImpl.class);
    
    private final SurveyScaleRepository scaleRepo;
    private final SurveyTemplateRepository templateRepo;
    private final SurveyQuestionRepository questionRepo;

    @PersistenceContext
    private EntityManager em;

    public SurveyServiceImpl(
            SurveyScaleRepository scaleRepo,
            SurveyTemplateRepository templateRepo,
            SurveyQuestionRepository questionRepo) {
        this.scaleRepo = scaleRepo;
        this.templateRepo = templateRepo;
        this.questionRepo = questionRepo;
    }

    @Override
    public List<SurveyDtos.TemplateSummary> listTemplates() {
        return templateRepo.findByStatusOrderByUpdatedAtDesc("ACTIVE")
            .stream()
            .map(t -> {
                SurveyDtos.TemplateSummary s = new SurveyDtos.TemplateSummary();
                s.id = t.getId();
                s.name = t.getName();
                s.version = t.getVersion();
                s.questionCount = questionRepo.findByTemplateIdOrderByQuestionNoAsc(t.getId()).size();
                return s;
            })
            .collect(Collectors.toList());
    }

    @Override
    public SurveyDtos.TemplateDetail getDefaultTemplate() {
        // 获取第一个ACTIVE状态的模板作为默认模板
        SurveyTemplate t = templateRepo.findByStatusOrderByUpdatedAtDesc("ACTIVE")
            .stream()
            .findFirst()
            .orElseThrow(() -> new BizException(40450, "no active survey template found"));

        return getTemplate(t.getId());
    }

    @Override
    public SurveyDtos.TemplateDetail getTemplate(Long id) {
        SurveyTemplate t = templateRepo.findById(id)
            .orElseThrow(() -> new BizException(40450, "survey template not found"));

        ObjectMapper objectMapper = new ObjectMapper();
        SurveyDtos.TemplateDetail d = new SurveyDtos.TemplateDetail();
        d.id = t.getId();
        d.name = t.getName();
        d.version = t.getVersion();
        d.description = t.getDescription();

        d.questions = questionRepo.findByTemplateIdOrderByQuestionNoAsc(id)
            .stream()
            .map(q -> {
                SurveyDtos.QuestionReq qr = new SurveyDtos.QuestionReq();
                qr.questionNo = q.getQuestionNo();
                qr.type = q.getType();
                qr.title = q.getTitle();
                qr.required = q.getRequired();
                String optionsJson = q.getOptionsJson();
                if (optionsJson == null || optionsJson.trim().isEmpty()) {
                    qr.options = null;
                } else {
                    try {
                        qr.options = objectMapper.readValue(
                            optionsJson,
                            new TypeReference<List<Object>>() {}
                        );
                    } catch (Exception e) {
                        // 数据库里 options_json 被写坏了，建议直接报 500（让你能发现数据问题）
                        throw new BizException(50051, "invalid options_json for questionId=" + q.getId());
                    }
                }
                return qr;
            })
            .collect(Collectors.toList());

        return d;
    }

    @Override
    @Transactional
    public SurveyTemplate saveTemplate(SurveyDtos.SaveTemplateReq req) {
    	// 1️⃣ 查或建 survey_scale
        SurveyScale scale = scaleRepo.findByCode(req.scaleCode)
            .orElseGet(() -> {
                SurveyScale s = new SurveyScale();
                s.setCode(req.scaleCode);
                s.setName(
                    req.scaleName != null ? req.scaleName : req.scaleCode
                );
                s.setDescription(req.scaleDescription);
                s.setStatus("ACTIVE");
                return scaleRepo.save(s);
            });

        // 2️⃣ 校验版本是否已存在
        if (templateRepo.existsByScaleIdAndVersion(scale.getId(), req.version)) {
            throw new BizException(40981, "template version exists");
        }

        // 3️⃣ 创建 template
        SurveyTemplate t = new SurveyTemplate();
        t.setScaleId(scale.getId());
        t.setName(req.templateName);
        t.setVersion(req.version);
        t.setDescription(req.templateDescription);
        t.setStatus("ACTIVE");
        t = templateRepo.save(t);

        ObjectMapper objectMapper = new ObjectMapper();
        // 4️⃣ 保存 questions（先不考虑 update，创建场景）
        for (QuestionReq q : req.questions) {
            SurveyQuestion sq = new SurveyQuestion();
            sq.setTemplateId(t.getId());
            sq.setQuestionNo(q.questionNo);
            sq.setType(q.type);
            sq.setTitle(q.title);
            sq.setRequired(q.required != null ? q.required : true);
            try {
				sq.setOptionsJson(
				    q.options != null ? objectMapper.writeValueAsString(q.options) : null
				);
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            questionRepo.save(sq);
        }

        return t;
    }

    @Override
    @Transactional
    public SurveyTemplate updateTemplate(Long id, SurveyDtos.UpdateTemplateReq req) {

        SurveyTemplate t = templateRepo.findById(id)
            .orElseThrow(() -> new BizException(40450, "survey template not found"));

        // 1. Update the template information
        t.setName(req.name);
        t.setDescription(req.description);
        t.setUpdatedAt(LocalDateTime.now());
        templateRepo.save(t);

        // 2. Delete all existing questions using native SQL
        em.createNativeQuery("DELETE FROM survey_question WHERE template_id = ?")
            .setParameter(1, id)
            .executeUpdate();

        // 3. Insert new questions using native SQL
        ObjectMapper objectMapper = new ObjectMapper();
        LocalDateTime now = LocalDateTime.now();
        
        for (SurveyDtos.QuestionReq q : req.questions) {
            String optionsJson = null;
            if (q.options != null) {
                try {
                    optionsJson = objectMapper.writeValueAsString(q.options);
                } catch (JsonProcessingException e) {
                    log.error("Failed to convert options to JSON for question {}", q.questionNo, e);
                    throw new BizException(500, "Failed to process question options");
                }
            }
            
            // Use native SQL to insert the question
            em.createNativeQuery("INSERT INTO survey_question (template_id, question_no, type, title, required, options_json, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)")
                .setParameter(1, id)
                .setParameter(2, q.questionNo)
                .setParameter(3, q.type)
                .setParameter(4, q.title)
                .setParameter(5, q.required != null ? q.required : true)
                .setParameter(6, optionsJson)
                .setParameter(7, now)
                .executeUpdate();
        }

        return t;
    }
    @Override
    @Transactional
    public void deleteTemplate(Long id) {
        SurveyTemplate t = templateRepo.findById(id)
            .orElseThrow(() -> new BizException(40450, "survey template not found"));
        
        // 先删除关联的问题
        em.createNativeQuery("DELETE FROM survey_question WHERE template_id = ?")
            .setParameter(1, id)
            .executeUpdate();
        
        // 再删除模板
        templateRepo.delete(t);
    }
}
