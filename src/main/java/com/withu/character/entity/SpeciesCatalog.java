package com.withu.character.entity;

import java.util.List;

/**
 * 선택 가능한 캐릭터 종류. 프론트 CharacterAvatar.jsx의 SPECIES_ORDER와 반드시 일치해야 한다
 * (없는 값을 저장하면 프론트가 기본 캐릭터로 대체해버려 사용자가 고른 캐릭터가 사라진다).
 */
public final class SpeciesCatalog {

    private SpeciesCatalog() {
    }

    public static final List<String> IDS = List.of("bat", "tit", "dino", "hedgehog");

    public static boolean exists(String species) {
        return IDS.contains(species);
    }
}
