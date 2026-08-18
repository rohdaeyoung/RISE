package com.withu.mission.service;

import com.withu.auth.entity.User;
import com.withu.auth.repository.UserRepository;
import com.withu.ai.LifestyleVisionAiClient;
import com.withu.ai.LifestyleVisionAiClient.LifestyleVerification;
import com.withu.character.service.ExpressionResolver;
import com.withu.file.service.FileStorageService;
import com.withu.global.common.GameConstants;
import com.withu.global.error.CustomException;
import com.withu.global.error.ErrorCode;
import com.withu.group.entity.GroupMember;
import com.withu.group.repository.GroupMemberRepository;
import com.withu.mission.dto.MissionDto.Response;
import com.withu.mission.dto.MissionDto.TodaySummary;
import com.withu.mission.entity.Mission;
import com.withu.mission.entity.MissionType;
import com.withu.mission.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MissionService {

    private final MissionRepository missionRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final ExpressionResolver expressionResolver;
    private final MissionSetCreator missionSetCreator;
    private final LifestyleVisionAiClient lifestyleVisionAiClient;
    private final FileStorageService fileStorageService;

    /**
     * 오늘 미션을 가져오되, 아직 없으면 만들어서 준다.
     *
     * <p>트랜잭션 밖에서 실행한다. 세트 생성은 {@link MissionSetCreator}가 자기 트랜잭션에서 처리하므로,
     * 동시 요청에 밀려 유니크 제약에 걸리더라도 이쪽 트랜잭션이 오염되지 않고 먼저 만들어진 세트를 다시 읽을 수 있다.
     * (브라우저가 15초마다 폴링해서 실제로 겹친다.)
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public TodaySummary generateToday(Long userId) {
        LocalDate today = LocalDate.now();
        if (!missionRepository.existsByUserIdAndMissionDate(userId, today)) {
            try {
                missionSetCreator.createTodaySet(userId, today);
            } catch (DataIntegrityViolationException e) {
                log.info("오늘 미션 세트가 이미 생성되어 있어 기존 세트를 사용합니다. userId={}", userId);
            }
        }
        return getToday(userId);
    }

    public TodaySummary getToday(Long userId) {
        List<Mission> missions = missionRepository.findByUserIdAndMissionDateOrderByIdAsc(userId, LocalDate.now());
        LocalTime now = LocalTime.now();
        List<Response> responses = missions.stream().map(m -> Response.from(m, now)).toList();
        int rate = missions.isEmpty() ? 0 : (int) Math.round(
                100.0 * missions.stream().filter(Mission::isDone).count() / missions.size());
        return new TodaySummary(responses, rate);
    }

    /**
     * 생활습관 미션을 인증 사진으로 완료 처리한다.
     *
     * <p>사진이 미션과 맞는지 AI가 판정한다. 예전에는 사진을 서버로 보내지도 않고 무조건 완료
     * 처리했는데, 그러면 걷기 미션에 아무 사진이나 올려도 인증되어 인증 자체가 의미가 없었다.
     */
    @Transactional
    public Response verifyLifestyleMission(Long userId, Long missionId, MultipartFile photo) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new CustomException(ErrorCode.MISSION_NOT_FOUND));
        if (!mission.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        if (mission.isDone()) {
            throw new CustomException(ErrorCode.MISSION_ALREADY_DONE);
        }
        if (!mission.isUnlocked(LocalTime.now())) {
            throw new CustomException(ErrorCode.MISSION_LOCKED);
        }
        requireImage(photo);
        if (fileStorageService.isAlreadyUsed(photo)) {
            throw new CustomException(ErrorCode.PHOTO_ALREADY_USED);
        }

        if (!verifySafely(photo, mission.getTitle()).achieved()) {
            throw new CustomException(ErrorCode.MISSION_PHOTO_MISMATCH);
        }

        // 판정이 끝난 뒤에 저장한다 — 미달성 사진까지 DB에 쌓을 이유가 없다.
        mission.complete(fileStorageService.store(photo));
        rewardCoins(userId);
        return Response.from(mission, LocalTime.now());
    }

    /** 사진이 아닌 파일은 AI를 부르기 전에 걸러낸다 (MealService.requireImage와 같은 이유). */
    private void requireImage(MultipartFile photo) {
        if (photo == null || photo.isEmpty()) {
            throw new CustomException(ErrorCode.NOT_AN_IMAGE);
        }
        String contentType = photo.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new CustomException(ErrorCode.NOT_AN_IMAGE);
        }
    }

    /**
     * AI가 잠깐 죽어도 스택트레이스 섞인 500을 주지 않는다. 다만 판정을 대충 통과시키지도 않는다 —
     * 그러면 AI가 죽은 동안 아무 사진이나 인증되기 때문이다. 재시도하도록 안내한다.
     */
    private LifestyleVerification verifySafely(MultipartFile photo, String missionTitle) {
        try {
            return lifestyleVisionAiClient.verify(photo, missionTitle);
        } catch (CustomException e) {
            throw e;
        } catch (RuntimeException e) {
            log.warn("생활습관 미션 인증 판정에 실패했습니다. mission={}", missionTitle, e);
            throw new CustomException(ErrorCode.MISSION_VERIFY_FAILED);
        }
    }

    /**
     * 이 사진으로 인증하게 될 식단 미션의 제목. 없으면 null.
     *
     * <p>AI에게 "오늘의 식단 미션을 달성했는지" 물으려면 그 미션이 무엇인지 알려줘야 한다.
     * 제목 없이 물어보면 AI가 기준을 스스로 지어내서, 목표에 맞는 식사인데도 미달성으로 판정되곤 했다.
     */
    public String pendingDietMissionTitle(Long userId) {
        return missionRepository
                .findFirstByUserIdAndMissionDateAndTypeAndDoneFalseOrderByIdAsc(userId, LocalDate.now(), MissionType.DIET)
                .map(Mission::getTitle)
                .orElse(null);
    }

    /**
     * 식단 인증이 달성으로 판정되면 오늘 미완료 식단 미션 중 하나를 완료 처리한다
     * (프론트 AppContext.jsx LOG_MEAL 리듀서와 동일한 규칙).
     */
    @Transactional
    public boolean completeFirstPendingDietMission(Long userId) {
        Mission mission = missionRepository
                .findFirstByUserIdAndMissionDateAndTypeAndDoneFalseOrderByIdAsc(userId, LocalDate.now(), MissionType.DIET)
                .orElse(null);
        if (mission == null) {
            return false;
        }
        mission.complete();
        rewardCoins(userId);
        return true;
    }

    /** 미션 1개 완료 보상 — 누적 코인과 이번 챌린지 사이클 점수에 함께 반영한다. */
    @Transactional
    public void rewardCoins(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        user.addCoins(GameConstants.MISSION_COIN_REWARD);
        groupMemberRepository.findByUserId(userId).ifPresent(member -> {
            member.addCyclePoints(GameConstants.MISSION_COIN_REWARD);
            // 달성률이 바뀌면 그룹원 전체의 순위가 흔들리므로 표정 캐시도 그룹 단위로 다시 계산한다.
            List<Long> groupUserIds = groupMemberRepository.findByGroupId(member.getGroup().getId()).stream()
                    .map(GroupMember::getUserId)
                    .toList();
            expressionResolver.refreshGroup(groupUserIds);
        });
    }

}
