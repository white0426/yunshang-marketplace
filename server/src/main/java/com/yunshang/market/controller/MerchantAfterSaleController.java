package com.yunshang.market.controller;

import com.yunshang.market.dto.ApiDtos.AfterSaleProcessRequest;
import com.yunshang.market.security.AuthPrincipal;
import com.yunshang.market.service.AfterSaleService;
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
@RequestMapping("/api/merchant/after-sales")
@PreAuthorize("hasRole('MERCHANT')")
public class MerchantAfterSaleController {
    private final AfterSaleService afterSaleService;

    public MerchantAfterSaleController(AfterSaleService afterSaleService) {
        this.afterSaleService = afterSaleService;
    }

    @PatchMapping("/{afterSaleId}")
    public Map<String, Boolean> process(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long afterSaleId,
            @Valid @RequestBody AfterSaleProcessRequest request
    ) {
        afterSaleService.process(principal, afterSaleId, request);
        return Map.of("success", true);
    }
}
