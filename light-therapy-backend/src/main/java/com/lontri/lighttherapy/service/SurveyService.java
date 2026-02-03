package com.lontri.lighttherapy.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lontri.lighttherapy.common.BizException;
import com.lontri.lighttherapy.dto.SurveyDtos;
import com.lontri.lighttherapy.entity.SurveyQuestion;
import com.lontri.lighttherapy.entity.SurveyResult;
import com.lontri.lighttherapy.entity.SurveyTemplate;
import com.lontri.lighttherapy.repository.SurveyQuestionRepository;
import com.lontri.lighttherapy.repository.SurveyResultRepository;
import com.lontri.lighttherapy.repository.SurveyTemplateRepository;
import com.lontri.lighttherapy.repository.SubjectRepository;
import com.lontri.lighttherapy.repository.UserRepository;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SurveyService {

    private final SurveyResultRepository surveyResultRepo;
    private final UserRepository userRepo;
    private final SubjectRepository subjectRepo;
    private final SurveyTemplateRepository templateRepo;
    private final SurveyQuestionRepository questionRepo;

    public SurveyService(SurveyResultRepository surveyResultRepo, 
                         UserRepository userRepo, 
                         SubjectRepository subjectRepo, 
                         SurveyTemplateRepository templateRepo, 
                         SurveyQuestionRepository questionRepo) {
        this.surveyResultRepo = surveyResultRepo;
        this.userRepo = userRepo;
        this.subjectRepo = subjectRepo;
        this.templateRepo = templateRepo;
        this.questionRepo = questionRepo;
    }

    @Transactional
    public SurveyTemplate createTemplate(SurveyDtos.CreateTemplateReq req) {
        if (templateRepo.existsByCodeAndVersion(req.code, req.version)) {
            throw new BizException(40930, "survey template code+version exists", HttpStatus.CONFLICT);
        }

        LocalDateTime now = LocalDateTime.now();

        SurveyTemplate t = new SurveyTemplate();
        t.setCode(req.code);
        t.setName(req.name);
        t.setVersion(req.version);
        t.setDescription(req.description);
        t.setStatus("ACTIVE");
        t.setCreatedAt(now);
        t.setUpdatedAt(now);

        t = templateRepo.save(t);

        // 可选：创建时带 questions
        if (req.questions != null && !req.questions.isEmpty()) {
            for (SurveyDtos.CreateQuestionReq q : req.questions) {
                if (questionRepo.existsByTemplateIdAndQuestionNo(t.getId(), q.questionNo)) {
                    throw new BizException(40931, "question_no exists: " + q.questionNo, HttpStatus.CONFLICT);
                }
                SurveyQuestion sq = new SurveyQuestion();
                sq.setTemplateId(t.getId());
                sq.setQuestionNo(q.questionNo);
                sq.setType(q.type);
                sq.setTitle(q.title);
                sq.setOptionsJson(q.optionsJson);
                sq.setRequired(q.required != null ? q.required : true);
                sq.setCreatedAt(now);
                questionRepo.save(sq);
            }
        }

        return t;
    }

    public Page<SurveyTemplate> listTemplates(Optional<String> code, Optional<String> status, Pageable pageable) {
        if (code.isPresent()) return templateRepo.findByCode(code.get(), pageable);
        if (status.isPresent()) return templateRepo.findByStatus(status.get(), pageable);
        return templateRepo.findAll(pageable);
    }

    public SurveyTemplate getTemplate(Long id) {
        return templateRepo.findById(id)
                .orElseThrow(() -> new BizException(40430, "survey template not found", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public SurveyTemplate updateTemplate(Long id, SurveyDtos.UpdateTemplateReq req) {
        SurveyTemplate t = getTemplate(id);
        t.setName(req.name);
        t.setDescription(req.description);
        if (req.status != null) t.setStatus(req.status);
        t.setUpdatedAt(LocalDateTime.now());
        return templateRepo.save(t);
    }

    @Transactional
    public SurveyQuestion addQuestion(Long templateId, SurveyDtos.CreateQuestionReq req) {
        if (questionRepo.existsByTemplateIdAndQuestionNo(templateId, req.questionNo)) {
            throw new BizException(40931, "question_no exists: " + req.questionNo, HttpStatus.CONFLICT);
        }

        SurveyQuestion q = new SurveyQuestion();
        q.setTemplateId(templateId);
        q.setQuestionNo(req.questionNo);
        q.setType(req.type);
        q.setTitle(req.title);
        q.setOptionsJson(req.optionsJson);
        q.setRequired(req.required != null ? req.required : true);
        q.setCreatedAt(LocalDateTime.now());
        return questionRepo.save(q);
    }

    public List<SurveyQuestion> listQuestions(Long templateId) {
        // 确保模板存在，避免直接查 questions 返回空导致误判
        getTemplate(templateId);
        return questionRepo.findByTemplateIdOrderByQuestionNoAsc(templateId);
    }

    @Transactional
    public SurveyQuestion updateQuestion(Long templateId, Long questionId, SurveyDtos.UpdateQuestionReq req) {
        getTemplate(templateId);

        SurveyQuestion q = questionRepo.findById(questionId)
                .orElseThrow(() -> new BizException(40431, "survey question not found", HttpStatus.NOT_FOUND));

        if (!q.getTemplateId().equals(templateId)) {
            throw new BizException(40932, "question not belongs to template", HttpStatus.CONFLICT);
        }

        // 若 questionNo 改了，需要避免冲突
        if (!q.getQuestionNo().equals(req.questionNo) &&
                questionRepo.existsByTemplateIdAndQuestionNo(templateId, req.questionNo)) {
            throw new BizException(40931, "question_no exists: " + req.questionNo, HttpStatus.CONFLICT);
        }

        q.setQuestionNo(req.questionNo);
        q.setType(req.type);
        q.setTitle(req.title);
        q.setOptionsJson(req.optionsJson);
        q.setRequired(req.required != null ? req.required : true);
        return questionRepo.save(q);
    }

    @Transactional
    public void deleteQuestion(Long templateId, Long questionId) {
        getTemplate(templateId);

        SurveyQuestion q = questionRepo.findById(questionId)
                .orElseThrow(() -> new BizException(40431, "survey question not found", HttpStatus.NOT_FOUND));

        if (!q.getTemplateId().equals(templateId)) {
            throw new BizException(40932, "question not belongs to template", HttpStatus.CONFLICT);
        }

        questionRepo.delete(q);
    }

    public List<SurveyDtos.SurveyResult> getMySurveyResults(Principal principal) {
        String username = principal.getName();
        return userRepo.findByUsername(username)
                .map(user -> {
                    // 通过userId查找subject，再通过subjectId查找问卷结果
                    return subjectRepo.findByUserId(user.getId())
                            .map(subject -> surveyResultRepo.findBySubjectIdOrderByFilledAtDesc(subject.getId()))
                            .orElseThrow(() -> new BizException(40411, "subject not found", HttpStatus.NOT_FOUND));
                })
                .orElseThrow(() -> new BizException(40410, "user not found", HttpStatus.NOT_FOUND))
                .stream()
                .map(this::mapToSurveyResultDto)
                .collect(Collectors.toList());
    }

    public List<SurveyDtos.SurveyResult> getUserSurveyResults(Long userId) {
        if (!userRepo.existsById(userId)) {
            throw new BizException(40410, "user not found", HttpStatus.NOT_FOUND);
        }
        // 通过userId查找subject，再通过subjectId查找问卷结果
        return subjectRepo.findByUserId(userId)
                .map(subject -> surveyResultRepo.findBySubjectIdOrderByFilledAtDesc(subject.getId()))
                .orElseThrow(() -> new BizException(40411, "subject not found", HttpStatus.NOT_FOUND))
                .stream()
                .map(this::mapToSurveyResultDto)
                .collect(Collectors.toList());
    }

    private SurveyDtos.SurveyResult mapToSurveyResultDto(SurveyResult result) {
        SurveyDtos.SurveyResult dto = new SurveyDtos.SurveyResult();
        dto.id = result.getId();
        dto.sessionId = result.getSessionId();
        dto.templateId = result.getTemplateId();
        dto.status = result.getStatus();
        dto.score = result.getScore();
        dto.rawJson = result.getRawJson();
        dto.filledAt = result.getFilledAt();
        return dto;
    }

}
