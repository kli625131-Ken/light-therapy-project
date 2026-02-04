package com.lontri.lighttherapy.repository;

import com.lontri.lighttherapy.entity.TreatmentSession;
import com.lontri.lighttherapy.enums.TreatmentSessionStatus;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TreatmentSessionRepository extends JpaRepository<TreatmentSession, Long> {

    @org.springframework.data.jpa.repository.Lock(javax.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from TreatmentSession s where s.id = :id")
    Optional<TreatmentSession> findByIdWithLock(@Param("id") Long id);

    Page<TreatmentSession> findBySubjectId(Long subjectId, Pageable pageable);

    Page<TreatmentSession> findByStatus(String status, Pageable pageable);

    boolean existsBySubjectIdAndStatus(Long subjectId, String status);

    Page<TreatmentSession> findByStatus(TreatmentSessionStatus status, Pageable pageable);

    Page<TreatmentSession> findBySubjectIdAndStatus(Long subjectId, TreatmentSessionStatus status, Pageable pageable);

    Optional<TreatmentSession> findFirstBySubjectIdAndStatus(Long subjectId, String status);

    Optional<TreatmentSession> findFirstBySubjectIdAndStatusOrderByStartTimeDesc(Long subjectId, String string);

    boolean existsBySubjectIdAndStatus(Long subjectId, TreatmentSessionStatus status);

    boolean existsBySubjectIdAndStatusAndIdNot(Long subjectId, TreatmentSessionStatus running, Long id);

    Optional<TreatmentSession> findFirstBySubjectIdAndStatusOrderByStartTimeDesc(
            Long subjectId, TreatmentSessionStatus status);

    @Query("SELECT s FROM TreatmentSession s WHERE " +
            "(:subjectId IS NULL OR s.subjectId = :subjectId) AND " +
            "(:status IS NULL OR s.status = :status) AND " +
            "(:start IS NULL OR s.startTime >= :start) AND " +
            "(:end IS NULL OR s.startTime <= :end) " +
            "ORDER BY s.updatedAt DESC, s.createdAt DESC")
    Page<TreatmentSession> search(
            @Param("subjectId") Long subjectId,
            @Param("status") String status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);

}
