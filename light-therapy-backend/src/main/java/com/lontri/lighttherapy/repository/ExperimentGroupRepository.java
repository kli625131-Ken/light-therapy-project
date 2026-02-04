package com.lontri.lighttherapy.repository;

import com.lontri.lighttherapy.entity.ExperimentGroup;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

public interface ExperimentGroupRepository extends JpaRepository<ExperimentGroup, Long> {
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);
    
    /**
     * 查询所有指定ID的实验组，并立即加载关联的量表模板
     */
    @Query("SELECT g FROM ExperimentGroup g LEFT JOIN FETCH g.surveyTemplates WHERE g.id IN :ids")
    List<ExperimentGroup> findAllWithSurveyTemplates(Set<Long> ids);

    /**
     * 根据ID查询单个实验组，并立即加载关联的量表模板
     */
    @Query("SELECT g FROM ExperimentGroup g LEFT JOIN FETCH g.surveyTemplates WHERE g.id = :id")
    java.util.Optional<ExperimentGroup> findByIdWithSurveyTemplates(Long id);
}
