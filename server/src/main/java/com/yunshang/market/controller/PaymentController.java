package com.yunshang.market.controller;

import com.yunshang.market.dto.ApiDtos.PaymentView;
import com.yunshang.market.service.CartService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final CartService cartService;

    public PaymentController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/{paymentNo}")
    public PaymentView get(@PathVariable String paymentNo) {
        return cartService.payment(paymentNo);
    }

    @PostMapping("/{paymentNo}/pay")
    public PaymentView pay(@PathVariable String paymentNo) {
        return cartService.pay(paymentNo);
    }
}
