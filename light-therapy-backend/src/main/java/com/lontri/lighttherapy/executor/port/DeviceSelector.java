package com.lontri.lighttherapy.executor.port;

import java.util.List;

public interface DeviceSelector {
    List<String> selectDeviceSnsForScheme(Long schemeId);
}

