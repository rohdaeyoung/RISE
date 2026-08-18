package com.withu.mission.dto;

import com.withu.mission.entity.Mission;
import com.withu.mission.entity.MissionType;

import java.time.LocalTime;
import java.util.List;

public class MissionDto {

    public record Response(
            Long id,
            MissionType type,
            String title,
            boolean done,
            LocalTime unlockTime,
            boolean unlocked
    ) {
        public static Response from(Mission mission, LocalTime now) {
            return new Response(
                    mission.getId(),
                    mission.getType(),
                    mission.getTitle(),
                    mission.isDone(),
                    mission.getUnlockTime(),
                    mission.isUnlocked(now)
            );
        }
    }

    public record TodaySummary(
            List<Response> missions,
            int achievementRate
    ) {
    }
}
