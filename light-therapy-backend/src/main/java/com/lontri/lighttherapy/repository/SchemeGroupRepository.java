package com.lontri.lighttherapy.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lontri.lighttherapy.entity.SchemeGroup;

public interface SchemeGroupRepository extends JpaRepository<SchemeGroup, Long> {
	List<SchemeGroup> findByGroupId(Long groupId);
	List<SchemeGroup> findBySchemeId(Long schemeId);
	List<SchemeGroup> findBySchemeIdIn(List<Long> schemeIds);
	@Query("select sg.schemeId from SchemeGroup sg where sg.groupId = :groupId")
	List<Long> findSchemeIdsByGroupId(@Param("groupId") Long groupId);
	void deleteBySchemeId(Long schemeId);

//	List<Scheme> findByIdInAndStatus(List<Long> ids, String status);
//	Page<Scheme> findByIdInAndStatus(List<Long> ids, String status, Pageable pageable);

}
