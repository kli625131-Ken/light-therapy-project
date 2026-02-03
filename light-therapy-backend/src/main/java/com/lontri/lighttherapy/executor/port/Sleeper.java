package com.lontri.lighttherapy.executor.port;

public interface Sleeper {
    void sleepSeconds(int seconds) throws InterruptedException;
}