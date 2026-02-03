package com.lontri.lighttherapy.device.codec;

import com.lontri.lighttherapy.executor.port.TreatmentDefaults;

public final class CommandParamTransform {

    private static TreatmentDefaults treatmentDefaults;

    // 静态初始化方法，由Spring调用
    public static void setTreatmentDefaults(TreatmentDefaults defaults) {
        treatmentDefaults = defaults;
    }

    // percent(0~100) -> DALI(0~254) -> 2 hex chars
    public static String percentToDaliHex(int percent) {
        return percentToDaliHex(percent, 1.0f); // 默认系数为1.0
    }

    /**
     * 将百分比转换为DALI(0~254)的十六进制表示
     * @param percent 百分比值(0~100)
     * @param coefficient 系数(可配置)
     * @return DALI值的十六进制表示(2位)
     */
    public static String percentToDaliHex(int percent, float coefficient) {
        int p = clamp(percent, 0, 100);
        // 应用系数
        float adjustedPercent = p * coefficient;
        // 确保调整后的百分比仍在0~100范围内
        int adjustedP = clamp((int) Math.round(adjustedPercent), 0, 100);
        int dali = 254 * adjustedP / 100;
        dali = clamp(dali, 0, 254);
        return toHexFixed(dali, 2); // 1 byte
    }

    /**
     * lux -> DALI值(0~254) 转成16进制字符串
     * @param lux lux值
     * @return 16进制字符串(2字节)
     */
    public static String luxToDaliHex(int lux) {
        // 使用配置的转换系数
        float coefficient = treatmentDefaults != null ? (float) treatmentDefaults.luxToPercentCoefficient() : 1.0f;
        return luxToDaliHex(lux, coefficient);
    }

    /**
     * 将lux转换为DALI(0~254)的十六进制表示
     * lux和百分比的关系: percent = (lux / 10000) * coefficient
     * @param lux 光照强度值(lux)
     * @param coefficient 可配置系数
     * @return DALI值的十六进制表示(2位)
     */
    public static String luxToDaliHex(int lux, float coefficient) {
        // 计算百分比: lux/10000*coefficient
        float percent = (lux / 10000.0f) * coefficient;
        // 转换为整数百分比并确保在0~100范围内
        int p = clamp((int) Math.round(percent), 0, 100);
        // 使用已有的百分比转DALI方法
        return percentToDaliHex(p, 1.0f); // 系数已经应用在百分比计算中，这里使用1.0
    }

    // cctK -> DT8(mired) -> payload A3{low}C3{high} (8 hex chars)
    public static String cctKToDt8Payload(int cctK) {
        if (cctK <= 0) throw new IllegalArgumentException("cctK must be > 0");
        int dt8 = (int) Math.floor(1_000_000.0 / cctK); // mired
        dt8 = clamp(dt8, 0, 0xFFFF);
        String k16 = toHexFixed(dt8, 4); // HHLL
        String gbits = k16.substring(0, 2); // HH
        String dbits = k16.substring(2, 4); // LL
        return ("A3" + dbits + "C3" + gbits).toUpperCase(); // A3LLC3HH
    }
    public static String cctKToDt8(int cctK) {
        if (cctK <= 0) throw new IllegalArgumentException("cctK must be > 0");
        int dt8 = (int) Math.floor((cctK - 3386) / 23.64); // mired
        dt8 = clamp(dt8, 0, 255);
        return toHexFixed(dt8, 2); 
    }
    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
    private static String toHexFixed(int value, int len) {
    	return toHexFixed(value,len,'0');
    }
    private static String toHexFixed(int value, int len, char fillChar) {
        String hex = Integer.toHexString(value);
        
        if (hex.length() >= len) {
            return hex;
        }
        
        // 填充到指定宽度
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len - hex.length(); i++) {
            sb.append(fillChar);
        }
        sb.append(hex);
        
        return sb.toString();
    }
}
