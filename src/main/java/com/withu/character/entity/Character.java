package com.withu.character.entity;

import com.withu.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

/**
 * 캐릭터는 회원가입 시 1회 생성되어 계정에 영구 귀속된다 (그룹이 바뀌어도 유지).
 */
@Entity
@Table(name = "characters")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Character extends BaseTimeEntity {

    private static final String DEFAULT_OUTFIT = "everyday";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false, length = 30)
    private String species;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Expression expression;

    @Column(nullable = false, length = 30)
    private String outfit;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "character_owned_outfits", joinColumns = @JoinColumn(name = "character_id"))
    @Column(name = "outfit_id")
    private Set<String> ownedOutfits = new HashSet<>();

    @Builder
    private Character(Long userId, String species) {
        this.userId = userId;
        this.species = species;
        this.expression = Expression.NORMAL;
        this.outfit = DEFAULT_OUTFIT;
        this.ownedOutfits = new HashSet<>(Set.of(DEFAULT_OUTFIT));
    }

    public void changeExpression(Expression expression) {
        this.expression = expression;
    }

    public void changeSpecies(String species) {
        this.species = species;
    }

    public boolean owns(String outfitId) {
        return ownedOutfits.contains(outfitId);
    }

    public void addOutfit(String outfitId) {
        ownedOutfits.add(outfitId);
    }

    public void wearOutfit(String outfitId) {
        this.outfit = outfitId;
    }
}
