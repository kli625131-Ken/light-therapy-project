package com.lontri.lighttherapy.dto;

import javax.validation.constraints.NotBlank;
import java.util.List;

public class GroupDtos {
    public static class CreateGroupReq {
        @NotBlank public String name;
        public String description;
        public List<Long> surveyTemplateIds;   // ken260129-修改内容：将单个量表ID修改为量表ID列表（可选）
        public List<Long> deviceIds;     // ✅ 修改：设备ID列表（可选）
    }

    public static class UpdateGroupReq {
        @NotBlank public String name;
        public String description;
        public String status;
        public List<Long> surveyTemplateIds;   // ken260129-修改内容：将单个量表ID修改为量表ID列表（可选）
    }
}
