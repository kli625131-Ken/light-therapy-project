package com.lontri.lighttherapy.controller;

import com.lontri.lighttherapy.common.ApiResponse;
import com.lontri.lighttherapy.device.DeviceInfo;
import com.lontri.lighttherapy.entity.Device;
import com.lontri.lighttherapy.service.DeviceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/devices", "/api/v1/devices"})
public class DeviceController {
    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    /**
     * 获取所有设备
     * @return 设备列表
     */
    @GetMapping
    public ApiResponse<List<Device>> getAllDevices() {
        return ApiResponse.ok(deviceService.getAllDevices());
    }

    /**
     * 根据ID获取设备
     * @param id 设备ID
     * @return 设备信息
     */
    @GetMapping("/{id}")
    public ApiResponse<Device> getDeviceById(@PathVariable Long id) {
        return ApiResponse.ok(deviceService.getById(id));
    }

    /**
     * 根据设备SN获取设备信息
     * @param deviceSn 设备序列号
     * @return 设备信息
     */
    @GetMapping("/sn/{deviceSn}")
    public ApiResponse<DeviceInfo> getDeviceBySn(@PathVariable String deviceSn) {
        return ApiResponse.ok(deviceService.getBySn(deviceSn));
    }
}