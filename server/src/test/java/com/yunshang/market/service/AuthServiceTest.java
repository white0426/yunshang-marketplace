package com.yunshang.market.service;

import com.yunshang.market.common.ApiException;
import com.yunshang.market.dto.ApiDtos.LoginResponse;
import com.yunshang.market.dto.ApiDtos.MerchantRegisterRequest;
import com.yunshang.market.dto.ApiDtos.MerchantRegisterResponse;
import com.yunshang.market.dto.ApiDtos.RegisterRequest;
import com.yunshang.market.entity.Merchant;
import com.yunshang.market.entity.SysUser;
import com.yunshang.market.mapper.MerchantMapper;
import com.yunshang.market.mapper.SysUserMapper;
import com.yunshang.market.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private SysUserMapper userMapper;
    @Mock
    private MerchantMapper merchantMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private BootstrapService bootstrapService;
    @InjectMocks
    private AuthService authService;

    @Test
    void registerCreatesConsumerAndReturnsLoginSession() {
        AtomicReference<SysUser> insertedUser = new AtomicReference<>();
        RegisterRequest request = new RegisterRequest(
                "测试用户",
                "NEW-USER@example.com",
                "13800000000",
                "secret1"
        );

        when(userMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode("secret1")).thenReturn("encoded-password");
        when(userMapper.insert(any(SysUser.class))).thenAnswer(invocation -> {
            insertedUser.set(invocation.getArgument(0));
            return 1;
        });
        when(userMapper.selectOne(any())).thenAnswer(invocation -> insertedUser.get());
        when(passwordEncoder.matches("secret1", "encoded-password")).thenReturn(true);
        when(jwtService.generateToken(any(SysUser.class))).thenReturn("jwt-token");

        LoginResponse response = authService.register(request);

        assertEquals("jwt-token", response.token());
        assertEquals("user", response.role());
        assertEquals("/", response.path());
        assertEquals("new-user@example.com", insertedUser.get().getEmail());
        assertEquals("USER", insertedUser.get().getRole());
        assertEquals("普通会员", insertedUser.get().getMemberLevel());
        verify(userMapper).addRole(insertedUser.get().getId(), "r_user");
    }

    @Test
    void registerRejectsExistingEmail() {
        RegisterRequest request = new RegisterRequest(
                "测试用户",
                "duplicate@example.com",
                "13800000000",
                "secret1"
        );
        when(userMapper.selectCount(any())).thenReturn(1L);

        assertThrows(ApiException.class, () -> authService.register(request));

        verify(userMapper, never()).insert(any(SysUser.class));
    }

    @Test
    void registerMerchantCreatesShopAndMerchantAccount() {
        AtomicReference<Merchant> insertedMerchant = new AtomicReference<>();
        AtomicReference<SysUser> insertedUser = new AtomicReference<>();
        MerchantRegisterRequest request = new MerchantRegisterRequest(
                "测试好物店",
                "测试店主",
                "NEW-SHOP@example.com",
                "13900000000",
                "杭州",
                "主营家居与生活好物",
                "secret1"
        );

        when(userMapper.selectCount(any())).thenReturn(0L);
        when(merchantMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode("secret1")).thenReturn("encoded-password");
        when(merchantMapper.insert(any(Merchant.class))).thenAnswer(invocation -> {
            insertedMerchant.set(invocation.getArgument(0));
            return 1;
        });
        when(userMapper.insert(any(SysUser.class))).thenAnswer(invocation -> {
            insertedUser.set(invocation.getArgument(0));
            return 1;
        });

        MerchantRegisterResponse response = authService.registerMerchant(request);

        assertTrue(response.pending());
        assertEquals("new-shop@example.com", insertedMerchant.get().getEmail());
        assertEquals("PENDING", insertedMerchant.get().getStatus());
        assertEquals(new BigDecimal("5.0"), insertedMerchant.get().getRating());
        assertEquals(insertedMerchant.get().getId(), insertedUser.get().getMerchantId());
        assertEquals("MERCHANT", insertedUser.get().getRole());
        verify(userMapper).addRole(insertedUser.get().getId(), "r_merchant");
    }
}
