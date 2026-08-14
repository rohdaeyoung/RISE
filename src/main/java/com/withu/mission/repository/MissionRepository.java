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

    /** 그룹을 떠날 때 그 사이클에 속한 미션을 지운다. 남겨두면 새 그룹에서 그대로 이어져 보인다. */
    void deleteByUserIdAndMissionDateGreaterThanEqual(Long userId, LocalDate from);

    List<Mission> findByUserId(Long userId);

    void deleteByUserId(Long userId);

    /** 챌린지 사이클 전체(시작일~종료일)의 미션 — 최종 달성률 계산에 사용. */
    List<Mission> findByUserIdInAndMissionDateBetween(List<Long> userIds, LocalDate from, LocalDate to);
}
