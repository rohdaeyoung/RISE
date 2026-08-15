package com.withu.feed.service;

import com.withu.auth.entity.User;
import com.withu.auth.repository.UserRepository;
import com.withu.feed.dto.FeedDto.Comment;
import com.withu.feed.dto.FeedDto.MemberReactions;
import com.withu.feed.dto.FeedDto.Response;
import com.withu.feed.entity.FeedComment;
import com.withu.feed.entity.FeedReaction;
import com.withu.feed.repository.FeedCommentRepository;
import com.withu.feed.repository.FeedReactionRepository;
import com.withu.global.error.CustomException;
import com.withu.global.error.ErrorCode;
import com.withu.group.entity.Group;
import com.withu.group.entity.GroupMember;
import com.withu.group.repository.GroupMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 그룹 피드의 반응·댓글 (PRD 8. 그룹 피드).
 *
 * <p>프론트가 먼저 화면을 만들었는데 브라우저 저장소에만 쌓고 있었다. 그래서 반응과 댓글이
 * <b>남긴 사람 기기에만 보였다.</b> 서로의 진행을 확인하며 동기부여를 받는 것이 그룹 피드의
 * 목적이므로, 그룹원 모두에게 보이려면 서버가 들고 있어야 한다.
 *
 * <p>모두 "오늘" 기준이다. 피드 자체가 오늘의 인증을 보여주는 화면이라, 어제 반응이 오늘 카드에
 * 남아 있으면 누가 오늘 응원했는지 알 수 없다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedService {

    private final FeedReactionRepository reactionRepository;
    private final FeedCommentRepository commentRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;

    public Response getFeed(Long userId) {
        Group group = groupOf(userId);
        LocalDate today = LocalDate.now();

        Map<String, MemberReactions> reactions = new LinkedHashMap<>();
        reactionRepository.findByGroupIdAndFeedDate(group.getId(), today).stream()
                .collect(Collectors.groupingBy(FeedReaction::getTargetUserId))
                .forEach((targetUserId, list) -> reactions.put(String.valueOf(targetUserId), new MemberReactions(
                        list.stream().collect(Collectors.groupingBy(FeedReaction::getEmoji, Collectors.counting())),
                        list.stream()
                                .filter(r -> r.getActorUserId().equals(userId))
                                .findFirst()
                                .map(FeedReaction::getEmoji)
                                .orElse(null))));

        List<FeedComment> comments = commentRepository.findByGroupIdAndFeedDateOrderByIdAsc(group.getId(), today);
        Map<Long, String> nicknames = nicknamesOf(comments.stream().map(FeedComment::getAuthorUserId).distinct().toList());

        return new Response(reactions, comments.stream()
                .map(c -> new Comment(
                        c.getId(),
                        c.getAuthorUserId(),
                        nicknames.get(c.getAuthorUserId()),
                        c.getAuthorUserId().equals(userId),
                        c.getText(),
                        c.getCreatedAt().toString()))
                .toList());
    }

    /**
     * 반응을 남기거나 지운다. 같은 이모지를 다시 누르면 취소, 다른 이모지를 누르면 교체다
     * (프론트 TOGGLE_REACTION 리듀서와 같은 규칙).
     */
    @Transactional
    public Response toggleReaction(Long userId, Long targetUserId, String emoji) {
        Group group = groupOf(userId);
        if (!groupMemberRepository.existsByGroupIdAndUserId(group.getId(), targetUserId)) {
            throw new CustomException(ErrorCode.NOT_GROUP_MEMBER);
        }

        LocalDate today = LocalDate.now();
        reactionRepository
                .findByGroupIdAndFeedDateAndActorUserIdAndTargetUserId(group.getId(), today, userId, targetUserId)
                .ifPresentOrElse(existing -> {
                    if (existing.getEmoji().equals(emoji)) {
                        reactionRepository.delete(existing);
                    } else {
                        existing.changeEmoji(emoji);
                    }
                }, () -> reactionRepository.save(FeedReaction.builder()
                        .groupId(group.getId())
                        .feedDate(today)
                        .actorUserId(userId)
                        .targetUserId(targetUserId)
                        .emoji(emoji)
                        .build()));

        reactionRepository.flush();
        return getFeed(userId);
    }

    @Transactional
    public Response addComment(Long userId, String text) {
        Group group = groupOf(userId);
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        commentRepository.save(FeedComment.builder()
                .groupId(group.getId())
                .feedDate(LocalDate.now())
                .authorUserId(userId)
                .text(trimmed.length() > FeedComment.MAX_LENGTH
                        ? trimmed.substring(0, FeedComment.MAX_LENGTH)
                        : trimmed)
                .build());
        return getFeed(userId);
    }

    private Map<Long, String> nicknamesOf(List<Long> userIds) {
        return userRepository.findAllById(userIds).stream()
                .filter(u -> u.getNickname() != null)
                .collect(Collectors.toMap(User::getId, User::getNickname));
    }

    private Group groupOf(Long userId) {
        return groupMemberRepository.findByUserId(userId)
                .map(GroupMember::getGroup)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_GROUP_MEMBER));
    }
}
