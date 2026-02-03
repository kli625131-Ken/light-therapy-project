package com.lontri.lighttherapy.infra.session;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.lontri.lighttherapy.entity.TreatmentSession;
import com.lontri.lighttherapy.entity.TreatmentSessionDevice;
import com.lontri.lighttherapy.executor.port.SessionStore;
import com.lontri.lighttherapy.repository.TreatmentSessionDeviceRepository;
import com.lontri.lighttherapy.repository.TreatmentSessionRepository;

@Component
public class JpaSessionStore implements SessionStore {
    private final TreatmentSessionRepository sessionRepo;
    private final TreatmentSessionDeviceRepository tsdRepo;

    public JpaSessionStore(TreatmentSessionRepository sessionRepo,
    		TreatmentSessionDeviceRepository tsdRepo) {
        this.sessionRepo = sessionRepo;
        this.tsdRepo = tsdRepo;
    }

    @Override
    public TreatmentSession getRequired(long sessionId) {
        return sessionRepo.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("session not found: " + sessionId));
    }

    @Override
    public boolean isRunning(long sessionId) {
        return sessionRepo.findById(sessionId)
                .map(s -> "RUNNING".equals(s.getStatus()))
                .orElse(false);
    }
    public List<String> getDeviceSns(long sessionId) {
        List<TreatmentSessionDevice> list = tsdRepo.findBySessionId(sessionId);
        if (!list.isEmpty()) {
            return list.stream()
                    .map(TreatmentSessionDevice::getDeviceSn)
                    .collect(Collectors.toList());
        }

        // fallback：旧字段（兼容历史数据）
        TreatmentSession s = getRequired(sessionId);
        return s.getDeviceSn() != null
                ? Collections.singletonList(s.getDeviceSn())
                : Collections.emptyList();
    }

}
