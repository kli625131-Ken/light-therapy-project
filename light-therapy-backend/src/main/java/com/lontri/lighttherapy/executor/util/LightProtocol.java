package com.lontri.lighttherapy.executor.util;

import java.util.Arrays;
import java.util.List;
public final class LightProtocol {
    private LightProtocol(){}

    // A108000002{亮度}EEEE
    public static String dimCmd(int protocolValue) {
        return "A108000002" + String.format("%04X", protocolValue & 0xFFFF) + "EEEE";
    }

    // 5A14FFFF0201EEEE{色温值}
    public static String cctCmd(int cctK) {
        return "5A14FFFF0201EEEE" + String.format("%04X", cctK & 0xFFFF);
    }

    public static List<String> build(int dimProtocolValue, int cctK) {
        return Arrays.asList(dimCmd(dimProtocolValue), cctCmd(cctK));
    }
}