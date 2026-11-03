package com.yunshang.market.controller;

import com.yunshang.market.dto.ApiDtos.ReviewRequest;
import com.yunshang.market.security.AuthPrincipal;
import com.yunshang.market.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@PreAuthorize("hasRole('USER')")
public class ReviewController {
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/products/{productId}")
    public Map<String, Boolean> reviewProduct(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable String productId,
            @Valid @RequestBody ReviewRequest request
    ) {
        reviewService.reviewProduct(principal, productId, request);
        return Map.of("success", true);
    }

    @PostMapping("/merchants/{merchantId}")
    public Map<String, Boolean> reviewMerchant(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable String merchantId,
            @Valid @RequestBody ReviewRequest request
    ) {
        reviewService.reviewMerchant(principal, merchantId, request);
        return Map.of("success", true);
    }
}
