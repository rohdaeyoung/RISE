package com.withu.auth.service;

import com.withu.auth.repository.UserRepository;
import com.withu.challenge.repository.ChallengeResultRepository;
import com.withu.challenge.repository.UserBadgeRepository;
import com.withu.character.repository.CharacterRepository;
import com.withu.file.service.FileStorageService;
import com.withu.global.error.CustomException;
import com.withu.global.error.ErrorCode;
import com.withu.group.entity.Group;
import com.withu.group.entity.GroupMember;
import com.withu.group.repository.GroupMemberRepository;
import com.withu.group.repository.GroupRepository;
import com.withu.meal.entity.Meal;
import com.withu.meal.repository.MealRepository;
import com.withu.mission.entity.Mission;
import com.withu.mission.repository.MissionRepository;
import com.withu.onboarding.repository.OnboardingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 계정 탈퇴 — 이 사용자의 데이터를 DB에서 전부 지운다.
 *
 * <p>예전에는 탈퇴 버튼이 브라우저 저장소만 비웠다. 화면에서는 사라졌지만 DB에는 계정이 그대로
 * 남아 있어서, 같은 이메일로 다시 가입하면 "이미 사용 중인 이메일"이 뜨고 전체 랭킹에도 계속
 * 나왔다. 탈퇴는 서버가 지워야 지워진다.
 *
 * <p>지우는 순서가 중요하다. 사진은 미션·식단 행이 가리키고 있어서 그 행을 먼저 지우면 어떤
 * 파일이 이 사람 것인지 알 수 없게 된다. 그래서 사진 주소를 먼저 모아두고 마지막에 파일을 지운다.
 *
 * <p>그룹은 남은 사람들의 것이므로 지우지 않는다. 다만 나간 사람이 방장이었다면 방장을 남은
 * 사람에게 넘기고, 마지막 한 명이 나가는 것이라면 빈 방이 되므로 방까지 정리한다
 * ({@code GroupService.leave()}와 같은 규칙).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountDeletionService {

    private final UserRepository userRepository;
    private final CharacterRepository characterRepository;
    private final MissionRepository missionRepository;
    private final MealRepository mealRepository;
    private final OnboardingRepository onboardingRepository;
    private final ChallengeResultRepository challengeResultRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupRepository groupRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public void delete(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        List<String> photoUrls = photoUrlsOf(userId);

        leaveGroupIfAny(userId);
        characterRepository.findByUserId(userId).ifPresent(characterRepository::delete);
        missionRepository.deleteByUserId(userId);
        mealRepository.deleteByUserId(userId);
        onboardingRepository.deleteByUserId(userId);
        challengeResultRepository.deleteByUserId(userId);
        userBadgeRepository.deleteByUserId(userId);
        userRepository.deleteById(userId);

        // 사진은 참조가 모두 사라진 뒤에 지운다. 실패해도 계정 삭제까지 되돌릴 이유는 없다.
        photoUrls.forEach(fileStorageService::deleteByUrl);

        log.info("계정을 삭제했습니다. userId={} 사진={}장", userId, photoUrls.size());
    }

    private List<String> photoUrlsOf(Long userId) {
        List<String> urls = new ArrayList<>();
        missionRepository.findByUserId(userId).stream().map(Mission::getPhotoUrl).forEach(urls::add);
        mealRepository.findByUserId(userId).stream().map(Meal::getPhotoUrl).forEach(urls::add);
        return urls.stream().filter(url -> url != null && !url.isBlank()).toList();
    }

    private void leaveGroupIfAny(Long userId) {
        GroupMember membership = groupMemberRepository.findByUserId(userId).orElse(null);
        if (membership == null) {
            return;
        }
        Group group = membership.getGroup();
        groupMemberRepository.deleteByGroupIdAndUserId(group.getId(), userId);
        groupMemberRepository.flush();

        List<GroupMember> remaining = groupMemberRepository.findByGroupId(group.getId());
        if (remaining.isEmpty()) {
            groupRepository.delete(group);
            return;
        }
        // 방장이 탈퇴하면 방장 자리가 없는 사람을 가리킨 채로 남는다. 남은 사람 중
        // 가장 먼저 들어온 사람에게 넘긴다.
        if (group.getHostUserId().equals(userId)) {
            group.changeHost(remaining.get(0).getUserId());
        }
    }
}
