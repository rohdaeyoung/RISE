package com.withu.mission.dto;

import com.withu.mission.entity.Mission;
import com.withu.mission.entity.MissionType;

import java.time.LocalTime;
import java.util.List;

public class MissionDto {

    /**
     * @param recognized AI가 인증 사진에서 본 것 (예: "공원 산책로"). 방금 인증한 응답에만 담기고,
     *                   미션 목록으로 다시 불러올 때는 null이다 — 판정 근거는 DB에 남기지 않는다.
     */
    public record Response(
            Long id,
            MissionType type,
            String title,
            boolean done,
            LocalTime unlockTime,
            boolean unlocked,
            String recognized
    ) {
        public static Response from(Mission mission, LocalTime now) {
            return of(mission, now, null);
        }

        public static Response of(Mission mission, LocalTime now, String recognized) {
            return new Response(
                    mission.getId(),
                    mission.getType(),
                    mission.getTitle(),
                    mission.isDone(),
                    mission.getUnlockTime(),
                    mission.isUnlocked(now),
                    recognized
            );
        }
    }

    public record TodaySummary(
            List<Response> missions,
            int achievementRate
    ) {
    }
}
