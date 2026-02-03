package com.lontri.lighttherapy.executor;

import java.util.List;

public interface TreatmentExecutor {
    void startScheme(long sessionId);
    void startManual(long sessionId, String deviceSn, int lux, int cctK);
    // ✅ 新增：支持多设备的手动启动
    void startManual(long sessionId, List<DeviceManualConfig> deviceConfigs);
    void pauseSession(Long sessionId);
    void resumeSession(Long sessionId);
    void stop(long sessionId, String reason);
    boolean isRunning(long sessionId);
    // ✅ 新增：单次手动控制（不改变 running map，也不启动线程）
    boolean manualControlOnce(long sessionId, String deviceSn,
            Integer dim, Integer sumDim, Integer skyDim, Integer cctK, Integer skyCctK,
            String source, String note);
    
    // ✅ 新增：设备手动配置类
    class DeviceManualConfig {
        private String deviceSn;
        private Integer dimP;
        private Integer sumDimP;
        private Integer skyDimP;
        private Integer cctK;
        private Integer skycctK;
        
        public DeviceManualConfig(String deviceSn, int dimP, Integer sumDimP, Integer skyDimP, Integer cctK, Integer skycctK) {
            this.deviceSn = deviceSn;
            this.dimP = dimP;
            this.sumDimP = sumDimP;
            this.skyDimP = skyDimP;
            this.cctK = cctK;
            this.skycctK = skycctK;
        }
        
        public String getDeviceSn() {
            return deviceSn;
        }
        
        public Integer getDimP() {
            return dimP;
        }
        
        public Integer getSumDimP() {
            return sumDimP;
        }
        
        public Integer getSkyDimP() {
            return skyDimP;
        }
        
        public Integer getCctK() {
            return cctK;
        }
        
        public Integer getSkycctK() {
            return skycctK;
        }
    }
}