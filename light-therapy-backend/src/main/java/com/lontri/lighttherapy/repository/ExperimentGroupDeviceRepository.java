package com.lontri.lighttherapy.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lontri.lighttherapy.entity.ExperimentGroupDevice;

public interface ExperimentGroupDeviceRepository extends JpaRepository<ExperimentGroupDevice, Long> {

    @Query("select egd.deviceId from ExperimentGroupDevice egd where egd.groupId = :groupId")
    List<Long> findDeviceIdsByGroupId(@Param("groupId") Long groupId);

    // 用于批量校验（推荐）
    @Query("select e.deviceId from ExperimentGroupDevice e where e.groupId = :groupId and e.deviceId in :deviceIds")
    List<Long> findExistingDeviceIds(@Param("groupId") Long groupId, @Param("deviceIds") List<Long> deviceIds);
    
    boolean existsByGroupIdAndDeviceId(Long groupId, Long deviceId);

    void deleteByGroupIdAndDeviceId(Long groupId, Long deviceId);
    List<ExperimentGroupDevice> findByGroupId(Long groupId);

}
