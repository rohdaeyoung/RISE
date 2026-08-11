package com.withu.mission.service;

import com.withu.global.error.CustomException;
import com.withu.group.entity.Group;
import com.withu.group.entity.GroupMember;
import com.withu.group.repository.GroupMemberRepository;
import com.withu.group.repository.GroupRepository;
import com.withu.mission.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 그룹이 정한 일일 미션 생성 시간이 지나면 그룹원들의 오늘 미션을 미리 만들어 둔다 (PRD 6.6 생성 시점).
 *
 * <p>이게 없으면 {@code missionHour}/{@code missionMinute} 설정은 저장만 될 뿐 아무 일도 하지 않고,
 * 미션은 사용자가 MY 화면을 열어 {@code POST /api/missions/today}를 호출하는 순간에야 만들어진다.
 * 그러면 그룹 피드에서 아직 앱을 켜지 않은 그룹원은 "미션 0개"로 보여 달성률이 왜곡된다.
 *
 * <p>사용자 요청 경로를 없애지는 않는다. 스케줄러가 뜨기 전(서버 재시작 직후 등)에 들어온 요청도
 * 즉시 미션을 받아야 하므로, 양쪽 모두 "없으면 만든다"로 동작하고 유니크 제약이 중복을 막는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyMissionScheduler {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final MissionRepository missionRepository;
    private final MissionSetCreator missionSetCreator;

    /**
     * 1분마다 확인한다. 그룹마다 생성 시간이 다르므로 특정 시각에 한 번 도는 cron으로는 맞출 수 없다.
     * 조회는 "오늘 미션이 없는 사람"만 걸러내는 가벼운 작업이고, 대상이 없으면 아무 일도 하지 않는다.
     */
    @Scheduled(fixedDelay = 60_000, initialDelay = 10_000)
    public void generateDueMissions() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        // GroupMember.group은 지연 로딩이라 트랜잭션 밖에서 접근할 수 없다. 그룹을 먼저 훑고
        // 생성 시간이 지난 그룹의 멤버만 꺼내면 프록시를 건드리지 않는다.
        int created = 0;
        for (Group group : groupRepository.findAll()) {
            if (!isDue(group, now)) {
                continue;
            }
            for (GroupMember member : groupMemberRepository.findByGroupId(group.getId())) {
                if (missionRepository.existsByUserIdAndMissionDate(member.getUserId(), today)) {
                    continue;
                }
                if (createFor(member.getUserId(), today)) {
                    created++;
                }
            }
        }
        if (created > 0) {
            log.info("일일 미션 생성 시간이 지나 미션 세트를 자동 생성했습니다. 인원={}", created);
        }
    }

    private boolean isDue(Group group, LocalTime now) {
        return !now.isBefore(LocalTime.of(group.getMissionHour(), group.getMissionMinute()));
    }

    /**
     * 한 사람이 실패해도 나머지 그룹원의 미션 생성까지 멈추면 안 되므로 개별로 감싼다.
     * 온보딩을 아직 마치지 않은 사람({@link CustomException})은 정상적인 건너뛰기 대상이다.
     */
    private boolean createFor(Long userId, LocalDate today) {
        try {
            missionSetCreator.createTodaySet(userId, today);
            return true;
        } catch (CustomException e) {
            return false;
        } catch (DataIntegrityViolationException e) {
            return false;
        } catch (RuntimeException e) {
            log.warn("자동 미션 생성에 실패했습니다. userId={}", userId, e);
            return false;
        }
    }
}
