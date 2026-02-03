package com.lontri.lighttherapy.executor.port;

public interface LuxCurveMapper {
    int luxToProtocol(int lux); // 0..10000 -> 0..65535
}