package com.lontri.lighttherapy.dto;

import java.time.LocalDateTime;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.lontri.lighttherapy.enums.SchemeScope;

public class SchemeDtos {
    public static class CreateSchemeReq {
        @NotBlank private String name;
        private String description;

        // 研究者创建组内方案一般固定为 GROUP
        // 如果你们未来要允许管理员创建 GLOBAL，才开放这个字段
        private SchemeScope scope = SchemeScope.GROUP;

        private Long groupId;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public SchemeScope getScope() {
			return scope;
		}

		public void setScope(SchemeScope scope) {
			this.scope = scope;
		}

		public Long getGroupId() {
			return groupId;
		}

		public void setGroupId(Long groupId) {
			this.groupId = groupId;
		}
        
    }

    public static class UpdateSchemeReq {
        @NotBlank public String name;
        public String description;
        @NotBlank public String status;
    }

    public static class UpdateSchemeWithStagesReq {
        @NotBlank public String name;
        public String description;
        @NotBlank public String status;

        // ✅ 多个组
        public List<@NotNull Long> groupId;

        @NotEmpty public List<@Valid UpdateStageReq> stages;

        public static class UpdateStageReq {
            @NotNull public Integer stageNo;
            @NotBlank public String name;

            @NotNull @Min(1)
            public Integer durationMinutes;

            public Integer lightIntensity;
            public Integer lightColorTemp;

            public String description;
            public List<DeviceConfig> deviceConfigs; // 设备独立配置

            public static class DeviceConfig {
                public Long id;  // 设备配置的主键ID，用于更新操作
                @NotBlank public String deviceSn;  // 设备序列号
                public Integer lightIntensity;     // 设备的亮度配置（可选）
                public Integer skyLightIntensity;  // 设备的天空光亮度配置（可选，仅485设备使用）
                public Integer lightColorTemp;     // 设备的色温配置（可选）
            }
        }
    }

    public static class CreateStageReq {
        @NotNull public Integer stageNo;
        @NotBlank public String name;

        @NotNull @Min(1)
        public Integer durationMinutes;   // ✅ 分钟

        public Integer lightIntensity;    // ✅ 亮度（按你 DB 可能是 lux 或百分比/单位自定）
        public Integer lightColorTemp;    // ✅ 色温（K）

        public String description;
        public List<DeviceConfig> deviceConfigs; // 设备独立配置

        public static class DeviceConfig {
            @NotBlank public String deviceSn;  // 设备序列号
            public Integer lightIntensity;     // 设备的亮度配置（可选）
            public Integer skyLightIntensity;  // 设备的天空光亮度配置（可选，仅485设备使用）
            public Integer lightColorTemp;     // 设备的色温配置（可选）
        }
    }


    public static class UpdateStageReq {
        @NotNull public Integer stageNo;
        @NotBlank public String name;

        @NotNull @Min(1)
        public Integer durationMinutes;   // ✅ 分钟

        public Integer lightIntensity;    // ✅ 亮度（按你 DB 可能是 lux 或百分比/单位自定）
        public Integer lightColorTemp;    // ✅ 色温（K）

        public String description;
        public List<DeviceConfig> deviceConfigs; // 设备独立配置

        public static class DeviceConfig {
            @NotBlank public String deviceSn;  // 设备序列号
            public Integer lightIntensity;     // 设备的亮度配置（可选）
            public Integer skyLightIntensity;  // 设备的天空光亮度配置（可选，仅485设备使用）
            public Integer lightColorTemp;     // 设备的色温配置（可选）
        }
    }
    public static class CreateSchemeWithStagesReq {
        @NotBlank public String name;
        public String description;

        // ✅ 多个组
        @NotEmpty public List<@NotNull Long> groupId;

        @NotEmpty public List<@Valid CreateStageReq> stages;

        public static class CreateStageReq {
            @NotNull public Integer stageNo;
            @NotBlank public String name;

            @NotNull @Min(1)
            public Integer durationMinutes;

            public Integer lightIntensity;
            public Integer lightColorTemp;

            public String description;
            public List<DeviceConfig> deviceConfigs; // 设备独立配置

            public static class DeviceConfig {
                @NotBlank public String deviceSn;  // 设备序列号
                public Integer lightIntensity;     // 设备的亮度配置（可选）
                public Integer skyLightIntensity;  // 设备的天空光亮度配置（可选，仅485设备使用）
                public Integer lightColorTemp;     // 设备的色温配置（可选）
            }
        }
    }

    public static class SchemeWithStagesDTO {
        private Long id;
        private Long groupId;
        private List<Long> groupIds;
        private List<ExperimentGroupDTO> groups;
        private SchemeScope scope;
        private String name;
        private String description;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private List<SchemeStageDTO> stages;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getGroupId() { return groupId; }
        public void setGroupId(Long groupId) { this.groupId = groupId; }
        public List<Long> getGroupIds() { return groupIds; }
        public void setGroupIds(List<Long> groupIds) { this.groupIds = groupIds; }
        public List<ExperimentGroupDTO> getGroups() { return groups; }
        public void setGroups(List<ExperimentGroupDTO> groups) { this.groups = groups; }
        public SchemeScope getScope() { return scope; }
        public void setScope(SchemeScope scope) { this.scope = scope; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
        public List<SchemeStageDTO> getStages() { return stages; }
        public void setStages(List<SchemeStageDTO> stages) { this.stages = stages; }
    }

    public static class ExperimentGroupDTO {
        private Long id;
        private String name;
        private String description;
        private List<Long> surveyTemplateIds;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<Long> getSurveyTemplateIds() { return surveyTemplateIds; }
        public void setSurveyTemplateIds(List<Long> surveyTemplateIds) { this.surveyTemplateIds = surveyTemplateIds; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }

    public static class SchemeStageDTO {
        private Long id;
        private Long schemeId;
        private Integer stageNo;
        private String name;
        private Integer startDayOffset;
        private Integer durationMinutes;
        private Integer lightIntensity;
        private Integer lightColorTemp;
        private Integer sessionCountPerDay;
        private String notes;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private List<SchemeStageDeviceConfigDTO> deviceConfigs;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getSchemeId() { return schemeId; }
        public void setSchemeId(Long schemeId) { this.schemeId = schemeId; }
        public Integer getStageNo() { return stageNo; }
        public void setStageNo(Integer stageNo) { this.stageNo = stageNo; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getStartDayOffset() { return startDayOffset; }
        public void setStartDayOffset(Integer startDayOffset) { this.startDayOffset = startDayOffset; }
        public Integer getDurationMinutes() { return durationMinutes; }
        public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
        public Integer getLightIntensity() { return lightIntensity; }
        public void setLightIntensity(Integer lightIntensity) { this.lightIntensity = lightIntensity; }
        public Integer getLightColorTemp() { return lightColorTemp; }
        public void setLightColorTemp(Integer lightColorTemp) { this.lightColorTemp = lightColorTemp; }
        public Integer getSessionCountPerDay() { return sessionCountPerDay; }
        public void setSessionCountPerDay(Integer sessionCountPerDay) { this.sessionCountPerDay = sessionCountPerDay; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
        public List<SchemeStageDeviceConfigDTO> getDeviceConfigs() { return deviceConfigs; }
        public void setDeviceConfigs(List<SchemeStageDeviceConfigDTO> deviceConfigs) { this.deviceConfigs = deviceConfigs; }
    }

    public static class SchemeStageDeviceConfigDTO {
        private Long id;
        private Long stageId;
        private Long deviceId;
        private String deviceSn;
        private Integer lightIntensity;
        private Integer lightColorTemp;
        private Integer skyLightIntensity;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getStageId() { return stageId; }
        public void setStageId(Long stageId) { this.stageId = stageId; }
        public Long getDeviceId() { return deviceId; }
        public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
        public String getDeviceSn() { return deviceSn; }
        public void setDeviceSn(String deviceSn) { this.deviceSn = deviceSn; }
        public Integer getLightIntensity() { return lightIntensity; }
        public void setLightIntensity(Integer lightIntensity) { this.lightIntensity = lightIntensity; }
        public Integer getLightColorTemp() { return lightColorTemp; }
        public void setLightColorTemp(Integer lightColorTemp) { this.lightColorTemp = lightColorTemp; }
        public Integer getSkyLightIntensity() { return skyLightIntensity; }
        public void setSkyLightIntensity(Integer skyLightIntensity) { this.skyLightIntensity = skyLightIntensity; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }

}
