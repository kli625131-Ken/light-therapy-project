package com.lontri.lighttherapy.service;

import com.lontri.lighttherapy.entity.TreatmentSession;

public interface TreatmentSessionStateMachine {

    enum Action {
        START, PAUSE, RESUME, END, CANCEL, FAIL
    }

    TreatmentSession apply(Long sessionId, Action action, String reason);
}
