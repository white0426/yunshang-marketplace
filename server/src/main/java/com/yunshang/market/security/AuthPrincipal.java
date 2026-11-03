package com.yunshang.market.security;

import java.security.Principal;

public record AuthPrincipal(
        String userId,
        String email,
        String role,
        String merchantId
) implements Principal {
    @Override
    public String getName() {
        return email;
    }
}
