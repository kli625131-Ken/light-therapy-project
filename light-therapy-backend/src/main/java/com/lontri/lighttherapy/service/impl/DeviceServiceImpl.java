package com.lontri.lighttherapy.service.impl;

import com.lontri.lighttherapy.common.BizException;
import com.lontri.lighttherapy.device.DeviceInfo;
import com.lontri.lighttherapy.entity.Device;
import com.lontri.lighttherapy.repository.DeviceRepository;
import com.lontri.lighttherapy.service.DeviceService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepo;

    public DeviceServiceImpl(DeviceRepository deviceRepo) {
        this.deviceRepo = deviceRepo;
    }

    @Override
    public DeviceInfo getBySn(String deviceSn) {
        Device d = deviceRepo.findByDeviceSn(deviceSn)
            .orElseThrow(() -> new BizException(40421, "device not found: " + deviceSn, HttpStatus.NOT_FOUND));

        DeviceInfo info = new DeviceInfo();
        info.setDeviceSn(d.getDeviceSn());
        info.setSendMode(d.getSendMode());
        info.setGatewayId(d.getGatewayId());
        info.setUdpIp(d.getUdpIp());
        info.setUdpPort(d.getUdpPort());
        return info;
    }

    @Override
    public Device getById(Long id) {
        return deviceRepo.findById(id)
            .orElseThrow(() -> new BizException(40422, "device not found by id: " + id, HttpStatus.NOT_FOUND));
    }

    @Override
    public List<Device> getAllDevices() {
        return deviceRepo.findAll();
    }
}
