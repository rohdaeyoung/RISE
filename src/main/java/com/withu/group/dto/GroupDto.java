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
