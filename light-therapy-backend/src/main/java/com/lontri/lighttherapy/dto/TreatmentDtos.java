package com.lontri.lighttherapy.dto;

import javax.validation.constraints.NotNull;

import java.util.List;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

public class TreatmentDtos {

    public static class CreateSessionReq {
        @NotNull public Long subjectId;
        @NotNull public Long schemeId;
        public Long stageId;        // 可选：如果你只想从某个stage开始
        // ✅ 新增：多设备
        public List<String> deviceSns;
        public String deviceSn;     // 必填建议：谁来执行
        
        public Long getSubjectId() {
            return subjectId;
        }
        
        public void setSubjectId(Long subjectId) {
            this.subjectId = subjectId;
        }
        
        public Long getSchemeId() {
            return schemeId;
        }
        
        public void setSchemeId(Long schemeId) {
            this.schemeId = schemeId;
        }
        
        public Long getStageId() {
            return stageId;
        }
        
        public void setStageId(Long stageId) {
            this.stageId = stageId;
        }
        
        public List<String> getDeviceSns() {
            return deviceSns;
        }
        
        public void setDeviceSns(List<String> deviceSns) {
            this.deviceSns = deviceSns;
        }
        
        public String getDeviceSn() {
            return deviceSn;
        }
        
        public void setDeviceSn(String deviceSn) {
            this.deviceSn = deviceSn;
        }
    }

    public static class ManualStartReq {
        @NotNull public Long subjectId;
        @NotNull public List<DeviceControlParam> deviceControls;
        
        public Long getSubjectId() {
            return subjectId;
        }
        
        public void setSubjectId(Long subjectId) {
            this.subjectId = subjectId;
        }
        
        public List<DeviceControlParam> getDeviceControls() {
            return deviceControls;
        }
        
        public void setDeviceControls(List<DeviceControlParam> deviceControls) {
            this.deviceControls = deviceControls;
        }
        
        // 设备控制参数内部类
        public static class DeviceControlParam {
            @NotBlank private String deviceSn;
            @Min(0) @Max(100)  // 亮度百分比 (0-100%)
            @NotNull private Integer dim;
            @Min(2700) @Max(10000) // 色温 (2700-10000K)
            @NotNull private Integer cctK;
            @Min(2700) @Max(10000) // 色温 (2700-10000K)
            @NotNull private Integer skyCctK;
            // 可选：485设备天空光亮度百分比 (0-100%)
            @Min(0) @Max(100)
            private Integer skyDim;
            @Min(0) @Max(100)
            private Integer sumDim;
            
            public String getDeviceSn() {
                return deviceSn;
            }
            
            public void setDeviceSn(String deviceSn) {
                this.deviceSn = deviceSn;
            }
            
            public Integer getDim() {
                return dim;
            }
            
            public void setDim(Integer dim) {
                this.dim = dim;
            }
            
            public Integer getCctK() {
                return cctK;
            }
            
            public void setCctK(Integer cctK) {
                this.cctK = cctK;
            }
            
            public Integer getSkyDim() {
                return skyDim;
            }
            
            public void setSkyDim(Integer skyDim) {
                this.skyDim = skyDim;
            }
            public Integer getSkyCctK() {
                return skyCctK;
            }
            public void setSkyCctK(Integer skyCctK) {
                this.skyCctK = skyCctK;
            }
            public Integer getSumDim() {
                return sumDim;
            }
            public void setSumDim(Integer sumDim) {
                this.sumDim = sumDim;
            }
        }
    }
    public static class AddEventReq {
        @NotBlank public String eventType; // START/END/DEVICE_MSG/ERROR/STAGE_START
        public String message;
        public String detailJson; // JSON string
    }
    public static class ManualControlReq {
        @NotNull private Long sessionId;
        @NotNull private List<DeviceControlParam> deviceControls;
        private String source;
        private String note;

        public Long getSessionId() {
			return sessionId;
		}

		public void setSessionId(Long sessionId) {
			this.sessionId = sessionId;
		}

		public List<DeviceControlParam> getDeviceControls() {
			return deviceControls;
		}

		public void setDeviceControls(List<DeviceControlParam> deviceControls) {
			this.deviceControls = deviceControls;
		}

		public String getSource() {
			return source;
		}

		public void setSource(String source) {
			this.source = source;
		}

		public String getNote() {
			return note;
		}

		public void setNote(String note) {
			this.note = note;
		}

        // 设备控制参数内部类
        public static class DeviceControlParam {
            @NotBlank private String deviceSn;
            @Min(0) @Max(100)  // 按你项目实际范围
            private Integer dim;
            @Min(2700) @Max(10000) // 按你项目实际范围
            private Integer cctK;
            @Min(2700) @Max(10000) // 按你项目实际范围
            private Integer skyCctK;
            // 可选：485设备天空光亮度百分比 (0-100%)
            @Min(0) @Max(100)
            private Integer sumDim;
            @Min(0) @Max(100)
            private Integer skyDim;

            public String getDeviceSn() {
                return deviceSn;
            }

            public void setDeviceSn(String deviceSn) {
                this.deviceSn = deviceSn;
            }

            public Integer getDim() {
                return dim;
            }

            public void setDim(Integer dim) {
                this.dim = dim;
            }

            public Integer getCctK() {
                return cctK;
            }

            public void setCctK(Integer cctK) {
                this.cctK = cctK;
            }
            public Integer getSkyCctK() {
                return skyCctK;
            }

            public void setSkyCctK(Integer skyCctK) {
                this.skyCctK = skyCctK;
            }
            public Integer getSumDim() {
                return sumDim;
            }

            public void setSumDim(Integer sumDim) {
                this.sumDim = sumDim;
            }
            
            public Integer getSkyDim() {
                return skyDim;
            }

            public void setSkyDim(Integer skyDim) {
                this.skyDim = skyDim;
            }
            
            @AssertTrue(message = "At least one of dim, cctK or skyDim must be provided")
            public boolean isAtLeastOneProvided() {
                return dim != null || cctK != null || skyDim != null;
            }
        }
    }
    
    // 研究者执行场景请求
    public static class ResearcherExecuteReq {
        @NotNull public List<DeviceControlParam> deviceControls;
        private String source;
        private String note;
        
        public List<DeviceControlParam> getDeviceControls() {
            return deviceControls;
        }
        
        public void setDeviceControls(List<DeviceControlParam> deviceControls) {
            this.deviceControls = deviceControls;
        }
        
        public String getSource() {
            return source;
        }
        
        public void setSource(String source) {
            this.source = source;
        }
        
        public String getNote() {
            return note;
        }
        
        public void setNote(String note) {
            this.note = note;
        }
        
        // 设备控制参数内部类
        public static class DeviceControlParam {
            @NotBlank private String deviceSn;
            @Min(0) @Max(100)  // 亮度百分比 (0-100%)
            @NotNull private Integer dim;
            @Min(2700) @Max(10000) // 色温 (2700-10000K)
            @NotNull private Integer cctK;
            // 可选：485设备天空光亮度百分比 (0-100%)
            @Min(0) @Max(100)
            private Integer skyDim;
            
            public String getDeviceSn() {
                return deviceSn;
            }
            
            public void setDeviceSn(String deviceSn) {
                this.deviceSn = deviceSn;
            }
            
            public Integer getDim() {
                return dim;
            }
            
            public void setDim(Integer dim) {
                this.dim = dim;
            }
            
            public Integer getCctK() {
                return cctK;
            }
            
            public void setCctK(Integer cctK) {
                this.cctK = cctK;
            }
            
            public Integer getSkyDim() {
                return skyDim;
            }
            
            public void setSkyDim(Integer skyDim) {
                this.skyDim = skyDim;
            }
        }
    }

}
