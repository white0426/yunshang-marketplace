package com.yunshang.market.controller;

import com.yunshang.market.dto.ApiDtos.CreateProductRequest;
import com.yunshang.market.dto.ApiDtos.MerchantProfileRequest;
import com.yunshang.market.dto.ApiDtos.PriceRequest;
import com.yunshang.market.dto.ApiDtos.ProductUpdateRequest;
import com.yunshang.market.dto.ApiDtos.ShipOrderRequest;
import com.yunshang.market.dto.ApiDtos.StatusRequest;
import com.yunshang.market.security.AuthPrincipal;
import com.yunshang.market.service.MerchantService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/merchant")
@PreAuthorize("hasRole('MERCHANT')")
public class MerchantController {
    private final MerchantService merchantService;

    public MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @PostMapping("/products")
    public Map<String, Object> createProduct(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody CreateProductRequest request
    ) {
        return Map.of(
                "success", true,
                "productId", merchantService.createProduct(principal, request).getId()
        );
    }

    @PatchMapping("/products/{productId}/price")
    public Map<String, Boolean> updatePrice(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable String productId,
            @Valid @RequestBody PriceRequest request
    ) {
        merchantService.updatePrice(principal, productId, request.price());
        return Map.of("success", true);
    }

    @PatchMapping("/products/{productId}/status")
    public Map<String, Boolean> updateProductStatus(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable String productId,
            @Valid @RequestBody StatusRequest request
    ) {
        merchantService.updateProductStatus(principal, productId, request.status());
        return Map.of("success", true);
    }

    @PatchMapping("/products/{productId}")
    public Map<String, Boolean> updateProduct(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable String productId,
            @Valid @RequestBody ProductUpdateRequest request
    ) {
        merchantService.updateProduct(principal, productId, request);
        return Map.of("success", true);
    }

    @PatchMapping("/profile")
    public Map<String, Boolean> updateProfile(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody MerchantProfileRequest request
    ) {
        merchantService.updateProfile(principal, request);
        return Map.of("success", true);
    }

    @PatchMapping("/orders/{orderId}/status")
    public Map<String, Boolean> updateOrderStatus(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable String orderId,
            @Valid @RequestBody StatusRequest request
    ) {
        merchantService.updateOrderStatus(principal, orderId, request.status());
        return Map.of("success", true);
    }

    @PatchMapping("/orders/{orderId}/ship")
    public Map<String, Object> shipOrder(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable String orderId,
            @Valid @RequestBody ShipOrderRequest request
    ) {
        String trackingNo = merchantService.shipOrder(principal, orderId, request);
        return Map.of(
                "success", true,
                "trackingNo", trackingNo,
                "shippingCompany", request.shippingCompany().trim()
        );
    }
}
