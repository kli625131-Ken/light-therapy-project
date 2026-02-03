package com.lontri.lighttherapy.dto;

import javax.validation.constraints.NotBlank;

public class UserDtos {
    public static class UserDTO {
        public Long id;
        public String username;
        public String role;
        public String status;
        // Subject info
        public String displayName;
        public String gender;
        public Integer age;
        public String group_name;
        public String subjectCode;
        public String contactInfo;
        public String diagnosisResult;
        public String subjectName;
    }

    public static class UpdateUserReq {
        @NotBlank public String role;
        @NotBlank public String status;
    }
    public static class UpdateSubjectReq {
        public String displayName;
        public String gender;
        public Integer age;
        public Long groupId;
        public String subjectCode;
        public String contactInfo;
        public String diagnosisResult;
        public java.time.LocalDate enrollmentDate;
        public String subjectName;
    }

    public static class CreateSubjectReq {
        @NotBlank
        public String username;
        @NotBlank
        public String password;
        @NotBlank
        public String subjectCode;
        public String displayName;
        public String gender;
        public Integer age;
        public Long groupId;
        public String contactInfo;
        public String diagnosisResult;
        public String subjectName;
        public java.time.LocalDate enrollmentDate;
    }

    public static class SubjectListResp {
        public Long id; // subject.id
        public String subjectCode;
        public String gender;
        public Integer age;
        public Long groupId;
        public String groupName;
        public String displayName;
        public String status;
        public String subjectName;
        public String contactInfo;
        public java.time.LocalDate enrollmentDate;
        public String diagnosisResult;

        public SubjectListResp(Long id, String subjectCode, String gender, Integer age, Long groupId,
                String groupName, String displayName, String status, String subjectName, String contactInfo,
                java.time.LocalDate enrollmentDate, String diagnosisResult) {
            this.id = id;
            this.subjectCode = subjectCode;
            this.gender = gender;
            this.age = age;
            this.groupId = groupId;
            this.groupName = groupName;
            this.displayName = displayName;
            this.status = status;
            this.subjectName = subjectName;
            this.contactInfo = contactInfo;
            this.enrollmentDate = enrollmentDate;
            this.diagnosisResult = diagnosisResult;
        }
    }
}
