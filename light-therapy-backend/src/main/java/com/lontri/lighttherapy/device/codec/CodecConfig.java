package com.lontri.lighttherapy.device.codec;

import com.lontri.lighttherapy.executor.port.TreatmentDefaults;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

/**
 * 设备编解码器配置类
 */
@Configuration
public class CodecConfig implements InitializingBean {

    private final TreatmentDefaults treatmentDefaults;

    @Autowired
    public CodecConfig(TreatmentDefaults treatmentDefaults) {
        this.treatmentDefaults = treatmentDefaults;
    }

    @Override
    public void afterPropertiesSet() {
        // 初始化CommandParamTransform的静态依赖
        CommandParamTransform.setTreatmentDefaults(treatmentDefaults);
    }
}