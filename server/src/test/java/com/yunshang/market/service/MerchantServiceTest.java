package com.yunshang.market.service;

import com.yunshang.market.dto.ApiDtos.ShipOrderRequest;
import com.yunshang.market.entity.ShopOrder;
import com.yunshang.market.mapper.MerchantMapper;
import com.yunshang.market.mapper.ProductMapper;
import com.yunshang.market.mapper.ShopOrderMapper;
import com.yunshang.market.security.AuthPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantServiceTest {
    @Mock
    private MerchantMapper merchantMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private ShopOrderMapper orderMapper;
    @InjectMocks
    private MerchantService merchantService;

    @Test
    void merchantCanOnlyShipPendingOrder() {
        AuthPrincipal principal = new AuthPrincipal(
                "mu1",
                "merchant@demo.com",
                "MERCHANT",
                "m1"
        );
        ShopOrder order = new ShopOrder();
        order.setId("o1");
        order.setMerchantId("m1");
        order.setStatus("待发货");
        when(orderMapper.selectById("o1")).thenReturn(order);

        merchantService.updateOrderStatus(principal, "o1", "待收货");

        assertEquals("待收货", order.getStatus());
        verify(orderMapper).updateById(order);
    }

    @Test
    void merchantCannotMarkOrderCompletedDirectly() {
        AuthPrincipal principal = new AuthPrincipal(
                "mu1",
                "merchant@demo.com",
                "MERCHANT",
                "m1"
        );
        ShopOrder order = new ShopOrder();
        order.setId("o1");
        order.setMerchantId("m1");
        order.setStatus("待发货");
        when(orderMapper.selectById("o1")).thenReturn(order);

        assertThrows(
                RuntimeException.class,
                () -> merchantService.updateOrderStatus(principal, "o1", "已完成")
        );

        verify(orderMapper, never()).updateById(any(ShopOrder.class));
    }

    @Test
    void merchantShippingStoresLogisticsInformation() {
        AuthPrincipal principal = new AuthPrincipal(
                "mu1",
                "merchant@demo.com",
                "MERCHANT",
                "m1"
        );
        ShopOrder order = new ShopOrder();
        order.setId("o1");
        order.setMerchantId("m1");
        order.setStatus("待发货");
        when(orderMapper.selectById("o1")).thenReturn(order);

        String trackingNo = merchantService.shipOrder(
                principal,
                "o1",
                new ShipOrderRequest("顺丰速运")
        );

        assertEquals("待收货", order.getStatus());
        assertEquals("顺丰速运", order.getShippingCompany());
        assertEquals(trackingNo, order.getTrackingNo());
        assertEquals(24, trackingNo.length());
        assertTrue(trackingNo.startsWith("SF"));
        assertTrue(trackingNo.matches("SF[0-9A-Z]{22}"));
        verify(orderMapper).updateById(order);
    }
}
