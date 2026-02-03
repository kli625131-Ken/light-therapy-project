package com.lontri.lighttherapy.infra.lux;

import com.lontri.lighttherapy.executor.port.LuxCurveMapper;
import org.springframework.stereotype.Component;

@Component
public class LinearLuxCurveMapper implements LuxCurveMapper {

    /**
     * 将 lux (0..10000) 映射到协议值 (0..65535)
     * 线性映射：protocol = round(lux/10000 * 65535)
     */
    @Override
    public int luxToProtocol(int lux) {
        int clamped = clamp(lux, 0, 10000);
        long v = Math.round(clamped * 65535.0 / 10000.0);
        return (int) clampLong(v, 0, 65535);
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private long clampLong(long v, long min, long max) {
        return Math.max(min, Math.min(max, v));
    }
}
