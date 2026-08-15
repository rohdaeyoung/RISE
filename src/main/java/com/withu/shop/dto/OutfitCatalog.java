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

    public static final String DEFAULT_ID = "everyday";

    public static final List<Item> ITEMS = List.of(
            new Item(DEFAULT_ID, "데일리 세트", 0, true),
            new Item("pajama", "파자마 세트", 30, false),
            new Item("sailor", "세일러 세트", 35, false),
            new Item("coat", "코트 세트", 40, false),
            new Item("detective", "탐정 세트", 50, false)
    );

    public static boolean exists(String outfitId) {
        return ITEMS.stream().anyMatch(i -> i.id().equals(outfitId));
    }

    /**
     * 카탈로그에 없는 의상은 기본 의상으로 바꿔 돌려준다.
     *
     * <p>의상 구성이 formal/picnic/sport에서 sailor/coat/detective로 교체되면서, 예전에 산 의상을
     * 입고 있던 사람이 DB에 남아 있다. 그대로 내려보내면 프론트에 해당 이미지 파일이 없어
     * 캐릭터가 깨진 이미지로 보인다. 없는 옷은 기본 옷으로 보이게 한다.
     */
    public static String normalize(String outfitId) {
        return exists(outfitId) ? outfitId : DEFAULT_ID;
    }

    public static int priceOf(String outfitId) {
        return ITEMS.stream().filter(i -> i.id().equals(outfitId)).findFirst()
                .map(Item::price)
                .orElse(Integer.MAX_VALUE);
    }
}
