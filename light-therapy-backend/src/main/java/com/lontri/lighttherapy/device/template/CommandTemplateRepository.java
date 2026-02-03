package com.lontri.lighttherapy.device.template;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CommandTemplateRepository extends JpaRepository<CommandTemplate, Long> {

	Optional<CommandTemplate> findByDeviceTypeAndFunctionCodeAndEnabled(
        String deviceType,
        String functionCode,
        boolean enabled
    );
}