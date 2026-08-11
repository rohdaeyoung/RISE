package com.withu.shop.dto;

import java.util.List;

/**
 * 의상 카탈로그. id와 가격은 프론트 ShopPage.jsx의 OUTFIT_SETS와 반드시 일치해야 한다
 * (프론트가 이미 확정된 기준). everyday는 기본 무료 보유 의상.
 */
public final class OutfitCatalog {

    private OutfitCatalog() {
    }

    public record Item(String id, String name, int price, boolean free) {
    }

    public static final List<Item> ITEMS = List.of(
            new Item("everyday", "데일리 세트", 0, true),
            new Item("formal", "포멀 세트", 30, false),
            new Item("pajama", "파자마 세트", 35, false),
            new Item("picnic", "피크닉 세트", 40, false),
            new Item("sport", "스포츠 세트", 50, false)
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
