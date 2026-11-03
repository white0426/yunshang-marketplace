package com.yunshang.market.controller;

import com.yunshang.market.dto.ApiDtos.CartAddRequest;
import com.yunshang.market.dto.ApiDtos.CartUpdateRequest;
import com.yunshang.market.security.AuthPrincipal;
import com.yunshang.market.service.CartService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@PreAuthorize("hasRole('USER')")
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/items")
    public Map<String, Boolean> add(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody CartAddRequest request
    ) {
        cartService.add(principal, request.productId(), request.quantity());
        return Map.of("success", true);
    }

    @PatchMapping("/items/{productId}")
    public Map<String, Boolean> update(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable String productId,
            @Valid @RequestBody CartUpdateRequest request
    ) {
        cartService.update(principal, productId, request.quantity());
        return Map.of("success", true);
    }

    @DeleteMapping("/items/{productId}")
    public Map<String, Boolean> remove(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable String productId
    ) {
        cartService.remove(principal, productId);
        return Map.of("success", true);
    }
}
