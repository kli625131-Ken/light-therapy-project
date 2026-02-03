package com.lontri.lighttherapy.dto;

public class SubjectDtos {
	public class SubjectResp {

	    public Long id;
	    public String code;      // 受试者编号（如 S0001）
	    public String name;      // 显示名
	    public String gender;    // M / F / UNKNOWN（可选）
	    public Integer age;      // 可选
	}
}
