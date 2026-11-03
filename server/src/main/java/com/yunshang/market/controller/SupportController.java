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
@RequestMapping("/api/support")
@PreAuthorize("hasRole('USER')")
public class SupportController {
    private final SupportService supportService;

    public SupportController(SupportService supportService) {
        this.supportService = supportService;
    }

    @GetMapping("/conversations")
    public List<SupportConversationView> conversations(
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return supportService.userConversations(principal);
    }

    @GetMapping("/conversations/{merchantId}")
    public SupportConversationDetail messages(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable String merchantId
    ) {
        return supportService.userMessages(principal, merchantId);
    }

    @PostMapping("/conversations/{merchantId}/messages")
    public SupportConversationDetail send(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable String merchantId,
            @Valid @RequestBody SupportMessageRequest request
    ) {
        return supportService.sendUserMessage(principal, merchantId, request);
    }
}
