package com.lontri.lighttherapy.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lontri.lighttherapy.entity.TreatmentEvent;
import com.lontri.lighttherapy.enums.EventType;

import java.time.LocalDateTime;

public class TreatmentEventFactory {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TreatmentEventFactory() {}

    /* ===== 无 detail ===== */
    public static TreatmentEvent of(Long sessionId,
                                    EventType type,
                                    String message) {
        TreatmentEvent e = new TreatmentEvent();
        e.setSessionId(sessionId);
        e.setEventTime(LocalDateTime.now());
        e.setEventType(type.code());
        e.setMessage(message);
        return e;
    }

    /* ===== 带 detail（对象 → JSON） ===== */
    public static TreatmentEvent of(Long sessionId,
                                    EventType type,
                                    String message,
                                    Object detail) {
        TreatmentEvent e = of(sessionId, type, message);
        if (detail != null) {
            try {
                e.setDetailJson(MAPPER.writeValueAsString(detail));
            } catch (JsonProcessingException ex) {
                // 兜底：不因事件失败影响主流程
                e.setDetailJson("{\"_error\":\"detail serialize failed\"}");
            }
        }
        return e;
    }

    /* ===== 直接传 JSON（你已经是 String 的情况） ===== */
    public static TreatmentEvent ofJson(Long sessionId,
                                        EventType type,
                                        String message,
                                        String detailJson) {
        TreatmentEvent e = of(sessionId, type, message);
        e.setDetailJson(detailJson);
        return e;
    }
}
