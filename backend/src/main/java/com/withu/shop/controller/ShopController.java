package com.withu.shop.controller;

import com.withu.global.common.ApiResponse;
import com.withu.global.security.SecurityUtil;
import com.withu.shop.dto.ShopDto.CatalogResponse;
import com.withu.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shop")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;

    @GetMapping("/outfits")
    public ApiResponse<CatalogResponse> getCatalog() {
        return ApiResponse.success(shopService.getCatalog(SecurityUtil.getCurrentUserId()));
    }

    @PostMapping("/outfits/{outfitId}/buy")
    public ApiResponse<CatalogResponse> buy(@PathVariable String outfitId) {
        return ApiResponse.success(shopService.buy(SecurityUtil.getCurrentUserId(), outfitId));
    }

    @PostMapping("/outfits/{outfitId}/wear")
    public ApiResponse<CatalogResponse> wear(@PathVariable String outfitId) {
        return ApiResponse.success(shopService.wear(SecurityUtil.getCurrentUserId(), outfitId));
    }
}
