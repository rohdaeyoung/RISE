package com.withu.group.service;

import com.withu.auth.entity.User;
import com.withu.auth.repository.UserRepository;
import com.withu.character.entity.Character;
import com.withu.character.entity.Expression;
import com.withu.character.repository.CharacterRepository;
import com.withu.character.service.ExpressionResolver;
import com.withu.global.error.CustomException;
import com.withu.global.error.ErrorCode;
import com.withu.group.dto.GroupDto.*;
import com.withu.group.entity.Group;
import com.withu.group.entity.GroupMember;
import com.withu.group.repository.GroupMemberRepository;
import com.withu.group.repository.GroupRepository;
import com.withu.meal.repository.MealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupService {

    private static final int MIN_MEMBERS = 2;
    private static final int MAX_MEMBERS = 4;
    private static final int MAX_CODE_GENERATION_ATTEMPTS = 10;

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final CharacterRepository characterRepository;
    private final GroupCodeGenerator codeGenerator;
    private final ExpressionResolver expressionResolver;
    private final MealRepository mealRepository;

    @Transactional
    public Response create(Long userId, CreateRequest request) {
        if (groupMemberRepository.existsByUserId(userId)) {
            throw new CustomException(ErrorCode.GROUP_ALREADY_JOINED);
        }
        Group group = Group.builder()
                .code(generateUniqueCode())
                .name(request.name())
                .hostUserId(userId)
                .missionHour(request.missionHour())
                .missionMinute(request.missionMinute())
                .build();
        groupRepository.save(group);
        groupMemberRepository.save(GroupMember.builder().group(group).userId(userId).build());
        return toResponse(group);
    }

    @Transactional
    public Response join(Long userId, JoinRequest request) {
        String code = request.code() == null ? "" : request.code().trim().toUpperCase();
        if (code.length() != 6) {
            throw new CustomException(ErrorCode.INVALID_GROUP_CODE);
        }
        if (groupMemberRepository.existsByUserId(userId)) {
            throw new CustomException(ErrorCode.GROUP_ALREADY_JOINED);
        }
        Group group = groupRepository.findByCode(code)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));
        if (groupMemberRepository.countByGroupId(group.getId()) >= MAX_MEMBERS) {
            throw new CustomException(ErrorCode.GROUP_FULL);
        }
        groupMemberRepository.save(GroupMember.builder().group(group).userId(userId).build());
        return toResponse(group);
    }

    public Response getMine(Long userId) {
        Group group = getGroupOfUser(userId);
        return toResponse(group);
    }

    @Transactional
    public void leave(Long userId) {
        Group group = getGroupOfUser(userId);
        groupMemberRepository.deleteByGroupIdAndUserId(group.getId(), userId);
        // 마지막 그룹원이 나가면 그룹 자체를 정리한다 (PRD 8. 방 운영).
        if (groupMemberRepository.countByGroupId(group.getId()) == 0) {
            groupRepository.delete(group);
        }
    }

    @Transactional
    public Response rename(Long userId, RenameRequest request) {
        Group group = getGroupOfUser(userId);
        group.rename(request.name());
        return toResponse(group);
    }

    @Transactional
    public Response changeMissionTime(Long userId, MissionTimeRequest request) {
        Group group = getGroupOfUser(userId);
        group.changeMissionTime(request.missionHour(), request.missionMinute());
        return toResponse(group);
    }

    /** 그룹원별 오늘의 최신 인증 사진 (없으면 항목 자체가 없음). */
    private Map<Long, String> todayPhotosOf(List<Long> userIds) {
        Map<Long, String> photos = new java.util.HashMap<>();
        mealRepository.findByUserIdInAndMealDateOrderByIdAsc(userIds, LocalDate.now()).stream()
                .filter(meal -> meal.getPhotoUrl() != null)
                // id 오름차순이라 나중 것이 앞의 값을 덮어써 "가장 최근 인증 사진"이 남는다.
                .forEach(meal -> photos.put(meal.getUserId(), meal.getPhotoUrl()));
        return photos;
    }

    private String generateUniqueCode() {
        for (int i = 0; i < MAX_CODE_GENERATION_ATTEMPTS; i++) {
            String code = codeGenerator.generate();
            if (!groupRepository.existsByCode(code)) {
                return code;
            }
        }
        throw new CustomException(ErrorCode.INTERNAL_ERROR);
    }

    private Group getGroupOfUser(Long userId) {
        GroupMember member = groupMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_GROUP_MEMBER));
        return member.getGroup();
    }

    private Response toResponse(Group group) {
        List<GroupMember> members = groupMemberRepository.findByGroupId(group.getId());
        List<Long> userIds = members.stream().map(GroupMember::getUserId).toList();

        Map<Long, User> usersById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, Character> charactersByUserId = characterRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(Character::getUserId, c -> c));
        // 표정은 저장된 값이 아니라 오늘 달성률+그룹 순위로 계산한다 (그룹 피드에서 실시간으로 바뀌어야 함).
        Map<Long, Expression> expressions = expressionResolver.todayExpressions(userIds);
        Map<Long, Integer> rates = expressionResolver.todayRates(userIds);
        Map<Long, String> photos = todayPhotosOf(userIds);

        List<MemberSummary> summaries = members.stream()
                .map(m -> {
                    User user = usersById.get(m.getUserId());
                    Character character = charactersByUserId.get(m.getUserId());
                    Expression expression = expressions.getOrDefault(m.getUserId(), Expression.NORMAL);
                    return new MemberSummary(
                            m.getUserId(),
                            user != null ? user.getNickname() : null,
                            character != null ? character.getSpecies() : null,
                            expression.name(),
                            character != null ? character.getOutfit() : null,
                            rates.getOrDefault(m.getUserId(), 0),
                            m.getCyclePoints(),
                            photos.get(m.getUserId())
                    );
                })
                .toList();

        return Response.of(group, summaries);
    }
}
