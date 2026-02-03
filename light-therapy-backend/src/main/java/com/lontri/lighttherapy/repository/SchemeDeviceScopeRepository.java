package com.lontri.lighttherapy.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lontri.lighttherapy.entity.SchemeDeviceScope;

import java.util.List;

public interface SchemeDeviceScopeRepository extends JpaRepository<SchemeDeviceScope, Long> {

	List<SchemeDeviceScope> findBySchemeId(Long schemeId);
    void deleteBySchemeId(Long schemeId);
}
