package com.withu.shop.dto;

import java.util.List;

/**
 * 의상 카탈로그. 프론트 assets/shop/*.png(formal-set, pajama-set, picnic-set, sport-set)와
 * 대응하며, everyday는 기본 무료 보유 의상 (DEVLOG.md 참고). 가격은 임시값 — 기획 확정 시 조정.
 */
public final class OutfitCatalog {

    private OutfitCatalog() {
    }

    public record Item(String id, String name, int price, boolean free) {
    }

    public static final List<Item> ITEMS = List.of(
            new Item("everyday", "데일리 세트", 0, true),
            new Item("formal-set", "포멀 세트", 150, false),
            new Item("pajama-set", "파자마 세트", 100, false),
            new Item("picnic-set", "피크닉 세트", 120, false),
            new Item("sport-set", "스포츠 세트", 120, false)
    );

    public static boolean exists(String outfitId) {
        return ITEMS.stream().anyMatch(i -> i.id().equals(outfitId));
    }

    public static int priceOf(String outfitId) {
        return ITEMS.stream().filter(i -> i.id().equals(outfitId)).findFirst()
                .map(Item::price)
                .orElse(Integer.MAX_VALUE);
    }
}
