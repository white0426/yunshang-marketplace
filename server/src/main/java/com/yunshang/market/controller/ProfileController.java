package com.yunshang.market.controller;

import com.yunshang.market.dto.ApiDtos.UserProfileRequest;
import com.yunshang.market.security.AuthPrincipal;
import com.yunshang.market.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final UserProfileService profileService;

    public ProfileController(UserProfileService profileService) {
        this.profileService = profileService;
    }

    @PatchMapping
    @PreAuthorize("hasRole('USER')")
    public Map<String, Boolean> update(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody UserProfileRequest request
    ) {
        profileService.update(principal, request);
        return Map.of("success", true);
    }
}
