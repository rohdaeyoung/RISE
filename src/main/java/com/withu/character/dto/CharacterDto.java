package com.withu.character.dto;

import com.withu.character.entity.Character;
import com.withu.character.entity.Expression;
import jakarta.validation.constraints.NotBlank;

import java.util.Set;

public class CharacterDto {

    public record CreateRequest(
            @NotBlank String species
    ) {
    }

    public record ChangeSpeciesRequest(
            @NotBlank String species
    ) {
    }

    public record Response(
            Long id,
            String species,
            Expression expression,
            String outfit,
            Set<String> ownedOutfits
    ) {
        public static Response from(Character character) {
            return new Response(
                    character.getId(),
                    character.getSpecies(),
                    character.getExpression(),
                    character.getOutfit(),
                    character.getOwnedOutfits()
            );
        }
    }
}
