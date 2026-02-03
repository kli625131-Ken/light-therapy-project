package com.lontri.lighttherapy.dto;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

import javax.validation.constraints.NotBlank;

public class AuthDtos {
    public static class LoginReq {
        @NotBlank public String username;
        @NotBlank public String password;
    }

    public static class LoginResp {
        public Long userId;
        public String role;
        public String token;

        // ✅ 新增
        public SubjectDto subject;          // role=SUBJECT 才会有
        public SubjectSnapshot snapshot;    // role=SUBJECT 才会有
    }

    public static class SubjectDto {
        public Long id;
        public String code;
        public String name;
        public String gender;
        public Integer age;
        public String contactInfo;
        public LocalDate enrollmentDate;
        public String diagnosisResult;
        public String subjectName;
        public String groupName;
        public String groupid;
        // ... 其他字段
    }

    public static class SubjectSnapshot {
        public String serverTime; // 你也可以用 LocalDateTime
        public CurrentSession currentSession;
    }

    public static class CurrentSession {
        public Long sessionId;
        public String status;

        public Long schemeId;
        public String schemeName;

        public Long stageId;
        public StageInfo stage;         // ✅ 当前阶段详情

        public String mode;             // "SCHEME" / "MANUAL"

        public List<DeviceInfo> devices; // ✅ 设备列表
        public LocalDateTime startTime;
    }
    public static class DeviceInfo {
        public String deviceSn;
        public String sendMode;   // UDP/TCP
        public String deviceType; // LONTRI/DALI/485
    }

    public static class StageInfo {
        public Long stageId;
        public Integer stageNo;
        public Integer lux;
        public Integer cctK;
        public Integer durationMinutes;
        public String name;
    }
}
