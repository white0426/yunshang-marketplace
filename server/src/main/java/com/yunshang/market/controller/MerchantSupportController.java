package com.yunshang.market.controller;

import com.yunshang.market.dto.ApiDtos.SupportConversationDetail;
import com.yunshang.market.dto.ApiDtos.SupportConversationView;
import com.yunshang.market.dto.ApiDtos.SupportMessageRequest;
import com.yunshang.market.security.AuthPrincipal;
import com.yunshang.market.service.SupportService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/merchant/support")
@PreAuthorize("hasRole('MERCHANT')")
public class MerchantSupportController {
    private final SupportService supportService;

    public MerchantSupportController(SupportService supportService) {
        this.supportService = supportService;
    }

    @GetMapping("/conversations")
    public List<SupportConversationView> conversations(
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return supportService.merchantConversations(principal);
    }

    @GetMapping("/conversations/{userId}")
    public SupportConversationDetail messages(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable String userId
    ) {
        return supportService.merchantMessages(principal, userId);
    }

    @PostMapping("/conversations/{userId}/messages")
    public SupportConversationDetail send(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable String userId,
            @Valid @RequestBody SupportMessageRequest request
    ) {
        return supportService.sendMerchantMessage(principal, userId, request);
    }
}
