package com.lontri.lighttherapy.dto;

import com.lontri.lighttherapy.entity.Device;
import com.lontri.lighttherapy.entity.ExperimentGroup;
import com.lontri.lighttherapy.entity.SurveyTemplate;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 分组响应DTO，包含设备信息
 */
@Data
public class GroupResponseDto {
    private Long id;
    private String name;
    private String description;
    private String status;
    private List<Long> surveyTemplateIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<Device> devices; // 分组下的设备列表

    public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

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

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public List<Long> getSurveyTemplateIds() {
		return surveyTemplateIds;
	}

	public void setSurveyTemplateIds(List<Long> surveyTemplateIds) {
		this.surveyTemplateIds = surveyTemplateIds;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	public List<Device> getDevices() {
		return devices;
	}

	public void setDevices(List<Device> devices) {
		this.devices = devices;
	}

	/**
     * 将ExperimentGroup实体转换为GroupResponseDto
     * @param group 实验分组实体
     * @param devices 设备列表
     * @return GroupResponseDto
     */
    public static GroupResponseDto fromEntity(ExperimentGroup group, List<Device> devices) {
        GroupResponseDto dto = new GroupResponseDto();
        dto.setId(group.getId());
        dto.setName(group.getName());
        dto.setDescription(group.getDescription());
        dto.setStatus(group.getStatus());
        // 处理surveyTemplates可能为null的情况
        if (group.getSurveyTemplates() != null) {
            dto.setSurveyTemplateIds(group.getSurveyTemplates().stream()
                    .map(SurveyTemplate::getId)
                    .collect(Collectors.toList()));
        } else {
            dto.setSurveyTemplateIds(new ArrayList<>());
        }
        dto.setCreatedAt(group.getCreatedAt());
        dto.setUpdatedAt(group.getUpdatedAt());
        dto.setDevices(devices);
        return dto;
    }
}