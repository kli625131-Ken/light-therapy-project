package com.lontri.lighttherapy.controller;

import com.lontri.lighttherapy.entity.Device;
import com.lontri.lighttherapy.enums.SendMode;
import com.lontri.lighttherapy.repository.DeviceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/test/device")
@PreAuthorize("permitAll()")
public class DeviceTestController {

    @Autowired
    private DeviceRepository deviceRepository;

    @GetMapping("/list")
    public List<Device> listDevices() {
        return deviceRepository.findAll();
    }

    @GetMapping("/{deviceSn}")
    public Device getDevice(@PathVariable String deviceSn) {
        Optional<Device> deviceOpt = deviceRepository.findByDeviceSn(deviceSn);
        return deviceOpt.orElse(null);
    }

    @PostMapping("/update")
    public Device updateDevice(@RequestBody DeviceUpdateRequest request) {
        Optional<Device> deviceOpt = deviceRepository.findByDeviceSn(request.getDeviceSn());
        
        if (deviceOpt.isPresent()) {
            Device device = deviceOpt.get();
            
            // 更新设备配置
            if (request.getSendMode() != null) {
                device.setSendMode(SendMode.valueOf(request.getSendMode()));
            }
            if (request.getUdpIp() != null) {
                device.setUdpIp(request.getUdpIp());
            }
            if (request.getUdpPort() != null) {
                device.setUdpPort(request.getUdpPort());
            }
            
            return deviceRepository.save(device);
        }
        
        return null;
    }

    // 内部类定义请求参数
    static class DeviceUpdateRequest {
        private String deviceSn;
        private String sendMode;
        private String udpIp;
        private Integer udpPort;

        // getter和setter
        public String getDeviceSn() {
            return deviceSn;
        }

        public void setDeviceSn(String deviceSn) {
            this.deviceSn = deviceSn;
        }

        public String getSendMode() {
            return sendMode;
        }

        public void setSendMode(String sendMode) {
            this.sendMode = sendMode;
        }

        public String getUdpIp() {
            return udpIp;
        }

        public void setUdpIp(String udpIp) {
            this.udpIp = udpIp;
        }

        public Integer getUdpPort() {
            return udpPort;
        }

        public void setUdpPort(Integer udpPort) {
            this.udpPort = udpPort;
        }
    }
}