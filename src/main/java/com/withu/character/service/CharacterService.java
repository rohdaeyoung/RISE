package com.withu.character.service;

import com.withu.character.dto.CharacterDto.*;
import com.withu.character.entity.Character;
import com.withu.character.repository.CharacterRepository;
import com.withu.global.error.CustomException;
import com.withu.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CharacterService {

    private final CharacterRepository characterRepository;

    @Transactional
    public Response create(Long userId, CreateRequest request) {
        if (characterRepository.existsByUserId(userId)) {
            throw new CustomException(ErrorCode.CHARACTER_ALREADY_EXISTS);
        }
        Character character = Character.builder()
                .userId(userId)
                .species(request.species())
                .build();
        characterRepository.save(character);
        return Response.from(character);
    }

    public Response getMine(Long userId) {
        return Response.from(getByUserId(userId));
    }

    @Transactional
    public Response changeSpecies(Long userId, ChangeSpeciesRequest request) {
        Character character = getByUserId(userId);
        character.changeSpecies(request.species());
        return Response.from(character);
    }

    private Character getByUserId(Long userId) {
        return characterRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHARACTER_NOT_FOUND));
    }
}
