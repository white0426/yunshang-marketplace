package com.yunshang.market.controller;

import com.yunshang.market.dto.ApiDtos.PriceRequest;
import com.yunshang.market.dto.ApiDtos.MerchantReviewRequest;
import com.yunshang.market.dto.ApiDtos.StatusRequest;
import com.yunshang.market.security.AuthPrincipal;
import com.yunshang.market.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PatchMapping("/merchants/{merchantId}/status")
    public Map<String, Boolean> updateMerchantStatus(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable String merchantId,
            @Valid @RequestBody StatusRequest request
    ) {
        adminService.updateMerchantStatus(principal, merchantId, request.status());
        return Map.of("success", true);
    }

    @PatchMapping("/merchants/{merchantId}/review")
    public Map<String, Boolean> reviewMerchant(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable String merchantId,
            @Valid @RequestBody MerchantReviewRequest request
    ) {
        adminService.reviewMerchant(principal, merchantId, request);
        return Map.of("success", true);
    }

    @PatchMapping("/products/{productId}/price")
    public Map<String, Boolean> updateProductPrice(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable String productId,
            @Valid @RequestBody PriceRequest request
    ) {
        adminService.updateProductPrice(principal, productId, request.price());
        return Map.of("success", true);
    }

    @PatchMapping("/products/{productId}/status")
    public Map<String, Boolean> updateProductStatus(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable String productId,
            @Valid @RequestBody StatusRequest request
    ) {
        adminService.updateProductStatus(principal, productId, request.status());
        return Map.of("success", true);
    }

    @PatchMapping("/users/{userId}/status")
    public Map<String, Boolean> updateUserStatus(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable String userId,
            @Valid @RequestBody StatusRequest request
    ) {
        adminService.updateUserStatus(principal, userId, request.status());
        return Map.of("success", true);
    }
}
