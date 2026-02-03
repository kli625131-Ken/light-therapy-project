package com.lontri.lighttherapy.infra.time;

import org.springframework.stereotype.Component;

import com.lontri.lighttherapy.executor.port.Clock;

@Component
public class SystemClock implements Clock {
    @Override public java.time.LocalDateTime now() { return java.time.LocalDateTime.now(); }
}