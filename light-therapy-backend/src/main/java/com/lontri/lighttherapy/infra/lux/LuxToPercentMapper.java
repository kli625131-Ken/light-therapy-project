package com.lontri.lighttherapy.infra.lux;

public final class LuxToPercentMapper {
	public static int linear(Integer lux) {
        if (lux == null) return 0;
        int x = Math.max(0, Math.min(10_000, lux));
        return (int) Math.round(x * 100.0 / 10_000.0);
    }
}
