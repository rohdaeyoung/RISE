package com.withu.shop.dto;

import java.util.List;
import java.util.Set;

public class ShopDto {

    public record OutfitItem(
            String id,
            String name,
            int price,
            boolean owned,
            boolean worn
    ) {
    }

    public record CatalogResponse(
            int coins,
            List<OutfitItem> outfits
    ) {
        public static CatalogResponse of(int coins, Set<String> owned, String worn) {
            List<OutfitItem> items = OutfitCatalog.ITEMS.stream()
                    .map(i -> new OutfitItem(i.id(), i.name(), i.price(), owned.contains(i.id()), i.id().equals(worn)))
                    .toList();
            return new CatalogResponse(coins, items);
        }
    }
}
