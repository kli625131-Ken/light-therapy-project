package com.lontri.lighttherapy.service.impl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lontri.lighttherapy.entity.TreatmentEvent;
import com.lontri.lighttherapy.enums.EventType;
import com.lontri.lighttherapy.repository.TreatmentEventRepository;
import com.lontri.lighttherapy.service.TreatmentEventService;

@Service
public class TreatmentEventServiceImpl implements TreatmentEventService {

    private final TreatmentEventRepository repo;

    public TreatmentEventServiceImpl(TreatmentEventRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional
    public TreatmentEvent record(Long sessionId, EventType type, String reason) {
        TreatmentEvent e = new TreatmentEvent();
        e.setSessionId(sessionId);

        // 关键：存 code（String）最稳，避免枚举重构影响历史数据
        e.setEventType(type.code());

        // 如果你表里有 reason 字段就写；没有就删掉这行
        e.setDetailJson(reason);

        e.setEventTime(LocalDateTime.now());
        return repo.save(e);
    }
}
