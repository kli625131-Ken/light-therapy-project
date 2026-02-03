package com.lontri.lighttherapy.infra.time;

import org.springframework.stereotype.Component;

import com.lontri.lighttherapy.executor.port.Sleeper;

@Component
public class ThreadSleeper implements Sleeper {
    @Override public void sleepSeconds(int seconds) throws InterruptedException {
        Thread.sleep(seconds * 1000L);
    }
}