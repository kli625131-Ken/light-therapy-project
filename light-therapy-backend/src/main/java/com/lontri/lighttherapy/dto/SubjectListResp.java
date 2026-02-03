package com.lontri.lighttherapy.dto;

import java.time.LocalDate;

public class SubjectListResp {
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
    public LocalDate enrollmentDate;
    public String diagnosisResult;

    public SubjectListResp(Long id, String subjectCode, String gender, Integer age, Long groupId, String groupName) {
        this.id = id;
        this.subjectCode = subjectCode;
        this.gender = gender;
        this.age = age;
        this.groupId = groupId;
        this.groupName = groupName;
    }

    public SubjectListResp(Long id, String subjectCode, String gender, Integer age, Long groupId, String groupName, 
                          String displayName, String status, String subjectName, String contactInfo, 
                          LocalDate enrollmentDate, String diagnosisResult) {
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
