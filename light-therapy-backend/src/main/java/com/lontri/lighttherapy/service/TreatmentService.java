package com.lontri.lighttherapy.service;

import com.lontri.lighttherapy.dto.TreatmentDtos;
import com.lontri.lighttherapy.dto.TreatmentDtos.CreateSessionReq;
import com.lontri.lighttherapy.dto.TreatmentDtos.ManualStartReq;
import com.lontri.lighttherapy.entity.TreatmentSession;
import com.lontri.lighttherapy.entity.TreatmentEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

import java.util.List;

import javax.validation.Valid;

public interface TreatmentService {
    TreatmentSession create(TreatmentDtos.CreateSessionReq req);
    Page<TreatmentSession> list(Long subjectId, String status, LocalDate startDate, LocalDate endDate, Pageable pageable);
    TreatmentSession get(Long id);

    TreatmentSession start(Long id);
    TreatmentSession end(Long id);     // ✅ 结束时生成 survey_result
    TreatmentSession fail(Long id, String reason);
    TreatmentSession cancel(Long id, String reason);

    TreatmentEvent addEvent(Long sessionId, TreatmentDtos.AddEventReq req);
    List<TreatmentEvent> listEvents(Long sessionId);
	TreatmentSession manualStart(ManualStartReq req);
	TreatmentSession createAndStart(CreateSessionReq req);
	TreatmentSession resume(Long id);
	TreatmentSession pause(Long id);
	void manualControl(@Valid TreatmentDtos.ManualControlReq req);
    void researcherExecute(@Valid TreatmentDtos.ResearcherExecuteReq req);
}
