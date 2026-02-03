package com.lontri.lighttherapy.executor.port;

import java.util.List;

import com.lontri.lighttherapy.entity.TreatmentSession;

public interface SessionStore {
    TreatmentSession getRequired(long sessionId);
    boolean isRunning(long sessionId);
	List<String> getDeviceSns(long sessionId);
}
