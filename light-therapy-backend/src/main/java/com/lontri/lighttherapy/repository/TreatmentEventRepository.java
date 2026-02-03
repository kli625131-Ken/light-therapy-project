package com.lontri.lighttherapy.repository;

import com.lontri.lighttherapy.entity.TreatmentEvent;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TreatmentEventRepository extends JpaRepository<TreatmentEvent, Long> {
    List<TreatmentEvent> findBySessionIdOrderByEventTimeAsc(Long sessionId);

}
