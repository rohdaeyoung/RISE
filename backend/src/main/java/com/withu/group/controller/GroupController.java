package com.withu.group.controller;

import com.withu.global.common.ApiResponse;
import com.withu.global.security.SecurityUtil;
import com.withu.group.dto.GroupDto.*;
import com.withu.group.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Response> create(@Valid @RequestBody CreateRequest request) {
        return ApiResponse.success(groupService.create(SecurityUtil.getCurrentUserId(), request));
    }

    @PostMapping("/join")
    public ApiResponse<Response> join(@Valid @RequestBody JoinRequest request) {
        return ApiResponse.success(groupService.join(SecurityUtil.getCurrentUserId(), request));
    }

    @GetMapping("/me")
    public ApiResponse<Response> getMine() {
        return ApiResponse.success(groupService.getMine(SecurityUtil.getCurrentUserId()));
    }

    /** 그룹 피드에서 사진을 눌렀을 때 열리는 그룹원 프로필 (PRD 6.12). */
    @GetMapping("/me/members/{userId}")
    public ApiResponse<MemberDetail> getMember(@PathVariable Long userId) {
        return ApiResponse.success(groupService.getMember(SecurityUtil.getCurrentUserId(), userId));
    }

    @DeleteMapping("/me")
    public ApiResponse<Void> leave() {
        groupService.leave(SecurityUtil.getCurrentUserId());
        return ApiResponse.empty();
    }

    @PatchMapping("/me/name")
    public ApiResponse<Response> rename(@Valid @RequestBody RenameRequest request) {
        return ApiResponse.success(groupService.rename(SecurityUtil.getCurrentUserId(), request));
    }

    @PatchMapping("/me/mission-time")
    public ApiResponse<Response> changeMissionTime(@Valid @RequestBody MissionTimeRequest request) {
        return ApiResponse.success(groupService.changeMissionTime(SecurityUtil.getCurrentUserId(), request));
    }
}
