package com.lontri.lighttherapy.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lontri.lighttherapy.entity.TreatmentSessionDevice;
public interface TreatmentSessionDeviceRepository extends JpaRepository<TreatmentSessionDevice, Long> {

	List<TreatmentSessionDevice> findBySessionId(Long sessionId);

    boolean existsBySessionIdAndDeviceId(Long sessionId, Long deviceId);

    void deleteBySessionId(Long sessionId);

	boolean existsBySessionIdAndDeviceSn(Long sessionId, String deviceSn);
}
