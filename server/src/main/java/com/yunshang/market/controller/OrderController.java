package com.yunshang.market.controller;

import com.yunshang.market.security.AuthPrincipal;
import com.yunshang.market.service.CartService;
import com.yunshang.market.dto.ApiDtos.CheckoutRequest;
import com.yunshang.market.dto.ApiDtos.PaymentView;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@PreAuthorize("hasRole('USER')")
public class OrderController {
    private final CartService cartService;

    public OrderController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/checkout")
    public PaymentView checkout(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody CheckoutRequest request
    ) {
        return cartService.checkout(principal, request);
    }

    @PostMapping("/payments/{paymentNo}/renew")
    public PaymentView renewPayment(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable String paymentNo
    ) {
        return cartService.renewPayment(principal, paymentNo);
    }

    @PatchMapping("/{orderId}/cancel")
    public Map<String, Boolean> cancel(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable String orderId
    ) {
        cartService.cancelOrder(principal, orderId);
        return Map.of("success", true);
    }

    @PatchMapping("/{orderId}/confirm")
    public Map<String, Boolean> confirm(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable String orderId
    ) {
        cartService.confirmReceipt(principal, orderId);
        return Map.of("success", true);
    }
}
