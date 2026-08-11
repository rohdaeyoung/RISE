package com.withu.group.service;

import com.withu.auth.entity.User;
import com.withu.auth.repository.UserRepository;
import com.withu.character.entity.Character;
import com.withu.character.repository.CharacterRepository;
import com.withu.global.error.CustomException;
import com.withu.global.error.ErrorCode;
import com.withu.group.dto.GroupDto.*;
import com.withu.group.entity.Group;
import com.withu.group.entity.GroupMember;
import com.withu.group.repository.GroupMemberRepository;
import com.withu.group.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public Response create(Long userId, CreateRequest request) {
        if (groupMemberRepository.findByUserId(userId).isPresent()) {
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
        if (groupMemberRepository.findByUserId(userId).isPresent()) {
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

        List<MemberSummary> summaries = members.stream()
                .map(m -> {
                    User user = usersById.get(m.getUserId());
                    Character character = charactersByUserId.get(m.getUserId());
                    return new MemberSummary(
                            m.getUserId(),
                            user != null ? user.getNickname() : null,
                            character != null ? character.getSpecies() : null,
                            character != null ? character.getExpression().name() : null,
                            character != null ? character.getOutfit() : null
                    );
                })
                .toList();

        return Response.of(group, summaries);
    }
}
