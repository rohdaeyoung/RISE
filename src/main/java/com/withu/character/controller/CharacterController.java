package com.withu.character.controller;

import com.withu.character.dto.CharacterDto.*;
import com.withu.character.service.CharacterService;
import com.withu.global.common.ApiResponse;
import com.withu.global.security.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/characters")
@RequiredArgsConstructor
public class CharacterController {

    private final CharacterService characterService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Response> create(@Valid @RequestBody CreateRequest request) {
        return ApiResponse.success(characterService.create(SecurityUtil.getCurrentUserId(), request));
    }

    @GetMapping("/me")
    public ApiResponse<Response> getMine() {
        return ApiResponse.success(characterService.getMine(SecurityUtil.getCurrentUserId()));
    }

    @PatchMapping("/me/species")
    public ApiResponse<Response> changeSpecies(@Valid @RequestBody ChangeSpeciesRequest request) {
        return ApiResponse.success(characterService.changeSpecies(SecurityUtil.getCurrentUserId(), request));
    }
}
