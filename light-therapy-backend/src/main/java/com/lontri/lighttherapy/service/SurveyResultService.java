package com.lontri.lighttherapy.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lontri.lighttherapy.common.BizException;
import com.lontri.lighttherapy.entity.SurveyResult;
import com.lontri.lighttherapy.repository.SurveyResultRepository;

@Service
public class SurveyResultService {

    private final SurveyResultRepository surveyResultRepo;

    public SurveyResultService(SurveyResultRepository surveyResultRepo) {
        this.surveyResultRepo = surveyResultRepo;
    }

    @Transactional
    public SurveyResult submit(Long id, String rawJson, BigDecimal score) {

        // 先尝试通过id查找SurveyResult
        SurveyResult r = surveyResultRepo.findById(id)
                .orElseGet(() -> {
                    // 如果找不到，尝试通过sessionId查找
                    java.util.List<SurveyResult> results = surveyResultRepo.findBySessionId(id);
                    if (results.isEmpty()) {
                        throw new BizException(40470, "survey result not found", HttpStatus.NOT_FOUND);
                    }
                    // Return the first element if found
                    return results.get(0);
                });

        if (!"PENDING".equals(r.getStatus())) {
            throw new BizException(40970, "survey already submitted", HttpStatus.CONFLICT);
        }
        try {
            new ObjectMapper().readTree(rawJson);
        } catch (Exception e) {
            throw new BizException(40070, "invalid survey answer json", HttpStatus.BAD_REQUEST);
        }
        // ✅ 这里才是 SUBMITTED
        r.setStatus("SUBMITTED");
        r.setRawJson(rawJson);
        r.setScore(score);
        r.setFilledAt(LocalDateTime.now());

        return surveyResultRepo.save(r);
    }
}
