package com.lontri.lighttherapy.executor.port;

public interface CommandGateway {
    void send(String deviceSn, String payload);
}
