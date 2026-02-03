package com.lontri.lighttherapy.entity;

import javax.persistence.*;

@Entity
@Table(name = "scheme_group")
public class SchemeGroup {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="scheme_id", nullable=false)
    private Long schemeId;

    @Column(name="group_id", nullable=false)
    private Long groupId;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getSchemeId() {
		return schemeId;
	}

	public void setSchemeId(Long schemeId) {
		this.schemeId = schemeId;
	}

	public Long getGroupId() {
		return groupId;
	}

	public void setGroupId(Long groupId) {
		this.groupId = groupId;
	}

    
}
