package com.withu.mission.repository;

import com.withu.mission.entity.Mission;
import com.withu.mission.entity.MissionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MissionRepository extends JpaRepository<Mission, Long> {

    List<Mission> findByUserIdAndMissionDateOrderByIdAsc(Long userId, LocalDate missionDate);

    List<Mission> findByUserIdInAndMissionDate(List<Long> userIds, LocalDate missionDate);

    Optional<Mission> findFirstByUserIdAndMissionDateAndTypeAndDoneFalseOrderByIdAsc(
            Long userId, LocalDate missionDate, MissionType type);

    boolean existsByUserIdAndMissionDate(Long userId, LocalDate missionDate);
}
