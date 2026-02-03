package com.lontri.lighttherapy.repository;

import com.lontri.lighttherapy.entity.Device;
import com.lontri.lighttherapy.enums.DeviceType;
import com.lontri.lighttherapy.enums.SendMode;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {
    Optional<Device> findByDeviceSn(String deviceSn);
    List<Device> findAll();
    List<Device> findBySendModeIn(List<SendMode> modes);

    @Query(
            "select d from Device d " +
            "where (:gatewayId is null or d.gatewayId = :gatewayId) " +
            "and (:deviceType is null or d.deviceType = :deviceType) " +
            "and (:sendMode is null or d.sendMode = :sendMode)"
        )
        List<Device> findByScope(@Param("gatewayId") String gatewayId,
                                 @Param("deviceType") DeviceType deviceType,
                                 @Param("sendMode") SendMode sendMode);
    List<Device> findByDeviceSnIn(List<String> deviceSns);
    List<Device> findByIdIn(List<Long> ids);
}
