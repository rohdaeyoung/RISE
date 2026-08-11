package com.withu.shop.service;

import com.withu.auth.entity.User;
import com.withu.auth.repository.UserRepository;
import com.withu.character.entity.Character;
import com.withu.character.repository.CharacterRepository;
import com.withu.global.error.CustomException;
import com.withu.global.error.ErrorCode;
import com.withu.shop.dto.OutfitCatalog;
import com.withu.shop.dto.ShopDto.CatalogResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShopService {

    private final UserRepository userRepository;
    private final CharacterRepository characterRepository;

    public CatalogResponse getCatalog(Long userId) {
        User user = getUser(userId);
        Character character = getCharacter(userId);
        return CatalogResponse.of(user.getCoins(), character.getOwnedOutfits(), character.getOutfit());
    }

    @Transactional
    public CatalogResponse buy(Long userId, String outfitId) {
        if (!OutfitCatalog.exists(outfitId)) {
            throw new CustomException(ErrorCode.OUTFIT_NOT_FOUND);
        }
        User user = getUser(userId);
        Character character = getCharacter(userId);
        if (character.owns(outfitId)) {
            throw new CustomException(ErrorCode.OUTFIT_ALREADY_OWNED);
        }
        int price = OutfitCatalog.priceOf(outfitId);
        if (user.getCoins() < price) {
            throw new CustomException(ErrorCode.NOT_ENOUGH_COINS);
        }
        user.spendCoins(price);
        character.addOutfit(outfitId);
        return CatalogResponse.of(user.getCoins(), character.getOwnedOutfits(), character.getOutfit());
    }

    @Transactional
    public CatalogResponse wear(Long userId, String outfitId) {
        Character character = getCharacter(userId);
        if (!character.owns(outfitId)) {
            throw new CustomException(ErrorCode.OUTFIT_NOT_OWNED);
        }
        character.wearOutfit(outfitId);
        User user = getUser(userId);
        return CatalogResponse.of(user.getCoins(), character.getOwnedOutfits(), character.getOutfit());
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private Character getCharacter(Long userId) {
        return characterRepository.findByUserId(userId).orElseThrow(() -> new CustomException(ErrorCode.CHARACTER_NOT_FOUND));
    }
}
