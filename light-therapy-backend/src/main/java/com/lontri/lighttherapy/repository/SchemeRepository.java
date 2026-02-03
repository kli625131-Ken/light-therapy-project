package com.lontri.lighttherapy.repository;

import com.lontri.lighttherapy.entity.Scheme;
import com.lontri.lighttherapy.enums.SchemeScope;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchemeRepository extends JpaRepository<Scheme, Long> {

    // 旧的（如果你准备彻底改用 scheme_group 中间表，这两个可以考虑后面删掉）
    Page<Scheme> findByGroupId(Long groupId, Pageable pageable);
    boolean existsByGroupId(Long groupId);

    boolean existsByName(String name);

    // scope 查询
    List<Scheme> findByScopeAndStatusOrderByIdDesc(SchemeScope scope, String status);
    List<Scheme> findByScopeAndGroupIdAndStatusOrderByIdDesc(SchemeScope scope, Long groupId, String status);

    Page<Scheme> findByScopeAndStatus(SchemeScope scope, String status, Pageable pageable);
    Page<Scheme> findByScopeAndGroupIdAndStatus(SchemeScope scope, Long groupId, String status, Pageable pageable);

    // id in + status（你现在 listRecommended() 需要它）
    List<Scheme> findByIdInAndStatus(List<Long> ids, String status);
    Page<Scheme> findByIdInAndStatus(List<Long> ids, String status, Pageable pageable);
    
    // 按状态查询所有方案
    Page<Scheme> findByStatus(String status, Pageable pageable);
}
