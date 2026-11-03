package com.yunshang.market.controller;

import com.yunshang.market.dto.ApiDtos.AccountView;
import com.yunshang.market.dto.ApiDtos.LoginRequest;
import com.yunshang.market.dto.ApiDtos.LoginResponse;
import com.yunshang.market.dto.ApiDtos.MerchantRegisterRequest;
import com.yunshang.market.dto.ApiDtos.MerchantRegisterResponse;
import com.yunshang.market.dto.ApiDtos.RegisterRequest;
import com.yunshang.market.security.AuthPrincipal;
import com.yunshang.market.service.AuthService;
import com.yunshang.market.service.BootstrapService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final BootstrapService bootstrapService;

    public AuthController(AuthService authService, BootstrapService bootstrapService) {
        this.authService = authService;
        this.bootstrapService = bootstrapService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    public LoginResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/register/merchant")
    public MerchantRegisterResponse registerMerchant(
            @Valid @RequestBody MerchantRegisterRequest request
    ) {
        return authService.registerMerchant(request);
    }

    @GetMapping("/me")
    public AccountView me(@AuthenticationPrincipal AuthPrincipal principal) {
        return bootstrapService.accountView(authService.verifyCurrent(principal));
    }

    @PostMapping("/logout")
    public Map<String, Boolean> logout(@AuthenticationPrincipal AuthPrincipal principal) {
        authService.logout(principal);
        return Map.of("success", true);
    }
}
