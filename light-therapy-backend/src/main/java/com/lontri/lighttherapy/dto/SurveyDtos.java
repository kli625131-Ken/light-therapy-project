package com.lontri.lighttherapy.dto;
import javax.validation.Valid;
import javax.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class SurveyDtos {
	public static class CreateTemplateReq {
        @NotBlank public String code;
        @NotBlank public String name;
        @NotNull @Min(1) public Integer version;
        public String description;

        // 可选：创建时直接带问题
        public List<@Valid CreateQuestionReq> questions;
    }

    public static class UpdateTemplateReq {
        @NotBlank public String name;
        public String description;
        @Pattern(regexp = "ACTIVE|ARCHIVED")
        public String status;
        @NotNull
        public List<QuestionReq> questions;
    }

    public static class CreateQuestionReq {
        @NotNull @Min(1) public Integer questionNo;
        @NotBlank public String type;     // SINGLE/MULTI/SCALE/TEXT/NUMBER
        @NotBlank public String title;
        public String optionsJson;        // JSON string
        public Boolean required = true;
    }

    public static class UpdateQuestionReq {
        @NotNull @Min(1) public Integer questionNo;
        @NotBlank public String type;
        @NotBlank public String title;
        public String optionsJson;
        public Boolean required = true;
    }
	/** 列表用 */
    public static class TemplateSummary {
        public Long id;
        public String name;
        public Integer version;
        public Integer questionCount;
    }

    /** 创建 / 更新（一次性保存模板 + 全量问题） */
    public static class SaveTemplateReq {

        // ===== Scale（量表）=====
        @NotBlank
        public String scaleCode;

        public String scaleName;
        public String scaleDescription;

        // ===== Template（模板）=====
        @NotBlank
        public String templateName;

        @NotNull
        public Integer version;

        public String templateDescription;

        // ===== Questions =====
        @NotEmpty
        public List<QuestionReq> questions;
    }

    public static class QuestionReq {
        @NotNull public Integer questionNo;
        @NotBlank public String type; // SCALE / SINGLE / MULTI / TEXT / NUMBER
        @NotBlank public String title;
        public Boolean required = true;
        public List<Object> options;
    }

    /** 详情返回 */
    public static class TemplateDetail {
        public Long id;
        public String name;
        public Integer version;
        public String description;
        public List<QuestionReq> questions;
    }
    public static class SubmitSurveyReq {

        /**
         * 问卷答案（JSON 字符串）
         * 示例：
         * {
         *   "1": 8,
         *   "2": "好",
         *   "3": "整体不错"
         * }
         */
        @NotBlank
        public String rawJson;

        /**
         * 可选：量表得分
         * - 如果前端已计算，直接传
         * - 如果后端算分，这个字段可以不传
         */
        public BigDecimal score;
    }

    public static class SurveyResult {
        public Long id;
        public Long userId;
        public Long sessionId;
        public Long templateId;
        public String templateName;
        public String status;
        public BigDecimal score;
        public String rawJson;
        public LocalDateTime createdAt;
        public LocalDateTime filledAt;
    }
}
