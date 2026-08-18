package com.withu.group.dto;

import com.withu.group.entity.Group;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public class GroupDto {

    public record CreateRequest(
            @Size(max = Group.MAX_NAME_LENGTH) String name,
            @Min(0) @Max(23) Integer missionHour,
            @Min(0) @Max(59) Integer missionMinute
    ) {
    }

    public record JoinRequest(
            @NotBlank String code
    ) {
    }

    public record RenameRequest(
            @NotBlank @Size(max = Group.MAX_NAME_LENGTH) String name
    ) {
    }

    public record MissionTimeRequest(
            @Min(0) @Max(23) int missionHour,
            @Min(0) @Max(59) int missionMinute
    ) {
    }

    public record MemberSummary(
            Long userId,
            String nickname,
            String species,
            String expression,
            String outfit,
            /** 오늘 미션 달성률(%) — 그룹 피드/그룹 내 순위 표시에 쓰인다. */
            int achievementRate,
            /** 이번 7일 사이클 동안 모은 점수 — 챌린지 종료 화면의 최종 순위 기준. */
            int points,
            /** 오늘 가장 최근에 인증한 사진 — 그룹 피드 카드에 표시. 인증 전이면 null. */
            String photo
    ) {
    }

    /**
     * 그룹원 프로필에 보여줄 오늘의 미션 한 건 (PRD 6.12).
     * 미션 제목과 달성 여부까지만 담고, 신체 정보나 식단 상세 분석은 담지 않는다 (PRD 12).
     */
    public record MemberMission(
            String type,
            String title,
            boolean done
    ) {
    }

    /**
     * 그룹원 프로필 화면 전체 (PRD 6.12 — 캐릭터 현재 상태, 식단/건강 미션 수행 결과).
     */
    public record MemberDetail(
            Long userId,
            String nickname,
            String species,
            String expression,
            String outfit,
            int achievementRate,
            int points,
            String photo,
            List<MemberMission> dietMissions,
            List<MemberMission> lifestyleMissions
    ) {
    }

    public record Response(
            Long id,
            String code,
            String name,
            Long hostUserId,
            int missionHour,
            int missionMinute,
            int currentDay,
            int challengeLengthDays,
            List<MemberSummary> members
    ) {
        public static Response of(Group group, List<MemberSummary> members) {
            return new Response(
                    group.getId(),
                    group.getCode(),
                    group.getName(),
                    group.getHostUserId(),
                    group.getMissionHour(),
                    group.getMissionMinute(),
                    group.currentDay(),
                    Group.CHALLENGE_LENGTH_DAYS,
                    members
            );
        }
    }
}
