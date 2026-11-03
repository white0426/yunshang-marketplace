package com.yunshang.market.controller;

import com.yunshang.market.dto.ApiDtos.AfterSaleCreateRequest;
import com.yunshang.market.security.AuthPrincipal;
import com.yunshang.market.service.AfterSaleService;
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
@RequestMapping("/api/after-sales")
@PreAuthorize("hasRole('USER')")
public class AfterSaleController {
    private final AfterSaleService afterSaleService;

    public AfterSaleController(AfterSaleService afterSaleService) {
        this.afterSaleService = afterSaleService;
    }

    @PostMapping("/orders/{orderId}")
    public Map<String, Boolean> create(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable String orderId,
            @Valid @RequestBody AfterSaleCreateRequest request
    ) {
        afterSaleService.create(principal, orderId, request);
        return Map.of("success", true);
    }
}
