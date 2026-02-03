package com.lontri.lighttherapy.infra.event;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.lontri.lighttherapy.entity.TreatmentEvent;
import com.lontri.lighttherapy.executor.port.EventSink;
import com.lontri.lighttherapy.repository.TreatmentEventRepository;

@Service
public class JpaEventSink implements EventSink {

    private final TreatmentEventRepository eventRepo;

    public JpaEventSink(TreatmentEventRepository eventRepo) {
        this.eventRepo = eventRepo;
    }

    @Override
    public void record(long sessionId, String type, String message, String extraJson) {
        TreatmentEvent e = new TreatmentEvent();
        e.setSessionId(sessionId);
        e.setEventType(type);
        e.setMessage(message);
        e.setDetailJson(extraJson);
        e.setEventTime(LocalDateTime.now());
        eventRepo.save(e);
    }
}
