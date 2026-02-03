package com.lontri.lighttherapy.infra.device;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.lontri.lighttherapy.entity.Device;
import com.lontri.lighttherapy.entity.SchemeDeviceScope;
import com.lontri.lighttherapy.executor.port.DeviceSelector;
import com.lontri.lighttherapy.repository.DeviceRepository;
import com.lontri.lighttherapy.repository.SchemeDeviceScopeRepository;

@Component
public class DbDeviceSelector implements DeviceSelector {

    private final SchemeDeviceScopeRepository scopeRepo;
    private final DeviceRepository deviceRepo;

    public DbDeviceSelector(SchemeDeviceScopeRepository scopeRepo,
                            DeviceRepository deviceRepo) {
        this.scopeRepo = scopeRepo;
        this.deviceRepo = deviceRepo;
    }

    @Override
    public List<String> selectDeviceSnsForScheme(Long schemeId) {
        List<SchemeDeviceScope> scopes = scopeRepo.findBySchemeId(schemeId);

        // ✅ 未配置范围 → 默认全选
        if (scopes == null || scopes.isEmpty()) {
            return deviceRepo.findAll()
                    .stream()
                    .map(Device::getDeviceSn)
                    .collect(Collectors.toList());
        }

        // ✅ 有范围 → OR 关系
        Set<String> result = new LinkedHashSet<>();
        for (SchemeDeviceScope sc : scopes) {
            List<Device> devices = deviceRepo.findByScope(
                    sc.getGatewayId(),
                    sc.getDeviceType(),
                    sc.getSendMode()
            );
            for (Device d : devices) {
                result.add(d.getDeviceSn());
            }
        }
        return new ArrayList<>(result);
    }
}
