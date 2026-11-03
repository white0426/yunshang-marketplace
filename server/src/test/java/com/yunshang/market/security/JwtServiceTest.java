package com.yunshang.market.security;

import com.yunshang.market.entity.SysUser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {
    private final JwtService jwtService = new JwtService(
            "test-secret-that-is-long-enough-for-hs256-signing-2026",
            1
    );

    @Test
    void generatedTokenContainsIdentityAndTokenVersion() {
        SysUser user = new SysUser();
        user.setId("u1");
        user.setEmail("user@demo.com");
        user.setTokenVersion(3);

        JwtService.TokenData token = jwtService.parse(jwtService.generateToken(user));

        assertThat(token.userId()).isEqualTo("u1");
        assertThat(token.email()).isEqualTo("user@demo.com");
        assertThat(token.tokenVersion()).isEqualTo(3);
    }
}
