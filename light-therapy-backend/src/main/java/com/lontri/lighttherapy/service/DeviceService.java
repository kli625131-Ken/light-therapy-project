package com.lontri.lighttherapy.service;

import com.lontri.lighttherapy.device.DeviceInfo;
import com.lontri.lighttherapy.entity.Device;

import java.util.List;

public interface DeviceService {
    DeviceInfo getBySn(String deviceSn);
    Device getById(Long id);
    List<Device> getAllDevices();
}
