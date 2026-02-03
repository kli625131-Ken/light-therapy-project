package com.lontri.lighttherapy.service;

import com.lontri.lighttherapy.entity.TreatmentEvent;
import com.lontri.lighttherapy.enums.EventType;

public interface TreatmentEventService {
    TreatmentEvent record(Long sessionId, EventType type, String reason);
}
