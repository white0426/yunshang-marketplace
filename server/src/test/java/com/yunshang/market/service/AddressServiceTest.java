package com.yunshang.market.service;

import com.yunshang.market.common.ApiException;
import com.yunshang.market.dto.ApiDtos.AddressRequest;
import com.yunshang.market.entity.UserAddress;
import com.yunshang.market.mapper.UserAddressMapper;
import com.yunshang.market.security.AuthPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {
    @Mock
    private UserAddressMapper addressMapper;
    @InjectMocks
    private AddressService addressService;

    private final AuthPrincipal principal =
            new AuthPrincipal("u1", "user@demo.com", "USER", null);
    private final AddressRequest request = new AddressRequest(
            "林知夏",
            "13739018827",
            "上海市 浦东新区",
            "示例路 18 号",
            false
    );

    @Test
    void firstAddressBecomesDefault() {
        AtomicReference<UserAddress> inserted = new AtomicReference<>();
        when(addressMapper.countByUser("u1")).thenReturn(0L);
        when(addressMapper.insert(any(UserAddress.class))).thenAnswer(invocation -> {
            UserAddress address = invocation.getArgument(0);
            address.setId(1L);
            inserted.set(address);
            return 1;
        });

        var view = addressService.create(principal, request);

        assertTrue(view.defaultAddress());
        assertEquals("上海市 浦东新区 示例路 18 号", view.fullAddress());
        assertEquals("u1", inserted.get().getUserId());
    }

    @Test
    void cannotModifyAnotherUsersAddress() {
        UserAddress address = new UserAddress();
        address.setId(8L);
        address.setUserId("u2");
        when(addressMapper.selectById(8L)).thenReturn(address);

        assertThrows(ApiException.class, () -> addressService.update(principal, 8L, request));

        verify(addressMapper, never()).updateById(any(UserAddress.class));
    }

    @Test
    void deletingDefaultAddressPromotesRemainingAddress() {
        UserAddress address = new UserAddress();
        address.setId(1L);
        address.setUserId("u1");
        address.setIsDefault(true);
        UserAddress remaining = new UserAddress();
        remaining.setId(2L);
        remaining.setUserId("u1");
        remaining.setIsDefault(false);

        when(addressMapper.selectById(1L)).thenReturn(address);
        when(addressMapper.selectMostRecent("u1")).thenReturn(remaining);

        addressService.delete(principal, 1L);

        verify(addressMapper).deleteById(1L);
        assertTrue(remaining.getIsDefault());
        verify(addressMapper).updateById(remaining);
    }
}
