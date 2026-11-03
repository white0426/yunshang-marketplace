package com.yunshang.market.controller;

import com.yunshang.market.dto.ApiDtos.BootstrapView;
import com.yunshang.market.dto.ApiDtos.StorefrontView;
import com.yunshang.market.security.AuthPrincipal;
import com.yunshang.market.service.BootstrapService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class BootstrapController {
    private final BootstrapService bootstrapService;

    public BootstrapController(BootstrapService bootstrapService) {
        this.bootstrapService = bootstrapService;
    }

    @GetMapping("/bootstrap")
    public BootstrapView bootstrap(@AuthenticationPrincipal AuthPrincipal principal) {
        return bootstrapService.load(principal);
    }

    @GetMapping("/storefront")
    public StorefrontView storefront() {
        return bootstrapService.loadStorefront();
    }
}
