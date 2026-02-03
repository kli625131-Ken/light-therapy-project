package com.lontri.lighttherapy.entity;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.lontri.lighttherapy.enums.DeviceType;

@Converter(autoApply = false)
public class DeviceTypeConverter implements AttributeConverter<DeviceType, String> {

    @Override
    public String convertToDatabaseColumn(DeviceType attribute) {
        if (attribute == null) return null;
        if (attribute == DeviceType._485) return "485";
        return attribute.name();
    }

    @Override
    public DeviceType convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        if ("485".equals(dbData)) return DeviceType._485;
        return DeviceType.valueOf(dbData);
    }
}
