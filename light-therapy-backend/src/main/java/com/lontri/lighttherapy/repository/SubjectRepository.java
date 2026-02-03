package com.lontri.lighttherapy.repository;

import com.lontri.lighttherapy.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    Optional<Subject> findByUserId(Long userId);
    boolean existsByGroupId(Long groupId);

    @Query("select s.groupId from Subject s where s.id = :subjectId")
    Long findGroupIdBySubjectId(@Param("subjectId") Long subjectId);

    Page<Subject> findAll(Pageable pageable);
    Optional<Subject> findBySubjectCode(String subjectCode);
}
