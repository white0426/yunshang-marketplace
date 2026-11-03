package com.yunshang.market.controller;

import com.yunshang.market.dto.ApiDtos.AddressRequest;
import com.yunshang.market.dto.ApiDtos.AddressView;
import com.yunshang.market.security.AuthPrincipal;
import com.yunshang.market.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/addresses")
@PreAuthorize("hasRole('USER')")
public class AddressController {
    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public List<AddressView> list(@AuthenticationPrincipal AuthPrincipal principal) {
        return addressService.list(principal);
    }

    @PostMapping
    public AddressView create(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody AddressRequest request
    ) {
        return addressService.create(principal, request);
    }

    @PatchMapping("/{addressId}")
    public AddressView update(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long addressId,
            @Valid @RequestBody AddressRequest request
    ) {
        return addressService.update(principal, addressId, request);
    }

    @PatchMapping("/{addressId}/default")
    public Map<String, Boolean> setDefault(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long addressId
    ) {
        addressService.setDefault(principal, addressId);
        return Map.of("success", true);
    }

    @DeleteMapping("/{addressId}")
    public Map<String, Boolean> delete(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long addressId
    ) {
        addressService.delete(principal, addressId);
        return Map.of("success", true);
    }
}
