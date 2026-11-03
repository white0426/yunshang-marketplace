package com.yunshang.market.service;

import com.yunshang.market.entity.OrderItem;
import com.yunshang.market.entity.ShopOrder;
import com.yunshang.market.mapper.CartItemMapper;
import com.yunshang.market.mapper.MerchantDailyStatMapper;
import com.yunshang.market.mapper.MerchantMapper;
import com.yunshang.market.mapper.OrderItemMapper;
import com.yunshang.market.mapper.ProductMapper;
import com.yunshang.market.mapper.ShopOrderMapper;
import com.yunshang.market.security.AuthPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {
    @Mock
    private CartItemMapper cartMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private MerchantMapper merchantMapper;
    @Mock
    private ShopOrderMapper orderMapper;
    @Mock
    private OrderItemMapper orderItemMapper;
    @Mock
    private MerchantDailyStatMapper statMapper;
    @InjectMocks
    private CartService cartService;

    @Test
    void cancelOrderRestoresInventorySalesAndStats() {
        AuthPrincipal principal = new AuthPrincipal("u1", "user@demo.com", "USER", null);
        ShopOrder order = new ShopOrder();
        order.setId("o1");
        order.setUserId("u1");
        order.setMerchantId("m1");
        order.setTotal(new BigDecimal("100.00"));
        order.setStatus("待发货");
        order.setCreatedAt(LocalDateTime.of(2026, 10, 31, 10, 0));

        OrderItem item = new OrderItem();
        item.setOrderId("o1");
        item.setProductId("p1");
        item.setQuantity(2);

        when(orderMapper.selectById("o1")).thenReturn(order);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(item));
        when(productMapper.restoreStock("p1", 2)).thenReturn(1);

        cartService.cancelOrder(principal, "o1");

        assertEquals("已取消", order.getStatus());
        verify(productMapper).restoreStock("p1", 2);
        verify(statMapper).subtractOrderStat(
                "m1",
                order.getCreatedAt().toLocalDate(),
                new BigDecimal("100.00")
        );
        verify(orderMapper).updateById(order);
    }

    @Test
    void cancelOrderRejectsShippedOrderWithoutRollback() {
        AuthPrincipal principal = new AuthPrincipal("u1", "user@demo.com", "USER", null);
        ShopOrder order = new ShopOrder();
        order.setId("o1");
        order.setUserId("u1");
        order.setStatus("待收货");
        when(orderMapper.selectById("o1")).thenReturn(order);

        assertThrows(
                RuntimeException.class,
                () -> cartService.cancelOrder(principal, "o1")
        );

        verify(productMapper, never()).restoreStock(any(), anyInt());
        verify(statMapper, never()).subtractOrderStat(any(), any(), any());
        verify(orderMapper, never()).updateById(any(ShopOrder.class));
    }

    @Test
    void cancelUnpaidOrderRestoresStockWithoutChangingSalesStats() {
        AuthPrincipal principal = new AuthPrincipal("u1", "user@demo.com", "USER", null);
        ShopOrder order = new ShopOrder();
        order.setId("o1");
        order.setUserId("u1");
        order.setMerchantId("m1");
        order.setTotal(new BigDecimal("100.00"));
        order.setStatus("待支付");

        OrderItem item = new OrderItem();
        item.setOrderId("o1");
        item.setProductId("p1");
        item.setQuantity(2);

        when(orderMapper.selectById("o1")).thenReturn(order);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(item));
        when(productMapper.restoreStock("p1", 2)).thenReturn(1);

        cartService.cancelOrder(principal, "o1");

        assertEquals("已取消", order.getStatus());
        verify(productMapper).restoreStock("p1", 2);
        verify(statMapper, never()).subtractOrderStat(any(), any(), any());
        verify(orderMapper).updateById(order);
    }

    @Test
    void payMovesAllPaymentOrdersToAwaitingShipmentAndIsIdempotent() {
        ShopOrder first = new ShopOrder();
        first.setId("o1");
        first.setMerchantId("m1");
        first.setTotal(new BigDecimal("100.00"));
        first.setStatus("待支付");
        first.setPaymentNo("PAY1");
        first.setPaymentExpiresAt(LocalDateTime.now().plusMinutes(20));

        ShopOrder second = new ShopOrder();
        second.setId("o2");
        second.setMerchantId("m2");
        second.setTotal(new BigDecimal("50.00"));
        second.setStatus("待支付");
        second.setPaymentNo("PAY1");
        second.setPaymentExpiresAt(first.getPaymentExpiresAt());

        when(orderMapper.selectByPaymentNoForUpdate("PAY1"))
                .thenReturn(List.of(first, second));

        cartService.pay("PAY1");
        cartService.pay("PAY1");

        assertEquals("待发货", first.getStatus());
        assertEquals("待发货", second.getStatus());
        verify(orderMapper, times(2)).updateById(any(ShopOrder.class));
        verify(statMapper, times(2)).addOrderStat(any(), any(), any());
    }

    @Test
    void payRejectsExpiredPaymentWithoutChangingOrders() {
        ShopOrder order = new ShopOrder();
        order.setId("o1");
        order.setStatus("待支付");
        order.setPaymentNo("PAY1");
        order.setPaymentExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(orderMapper.selectByPaymentNoForUpdate("PAY1"))
                .thenReturn(List.of(order));

        assertThrows(RuntimeException.class, () -> cartService.pay("PAY1"));

        verify(orderMapper, never()).updateById(any(ShopOrder.class));
        verify(statMapper, never()).addOrderStat(any(), any(), any());
    }

    @Test
    void renewPaymentExtendsExpiredPendingOrder() {
        AuthPrincipal principal = new AuthPrincipal("u1", "user@demo.com", "USER", null);
        ShopOrder order = new ShopOrder();
        order.setId("o1");
        order.setUserId("u1");
        order.setMerchantId("m1");
        order.setTotal(new BigDecimal("99.00"));
        order.setStatus("待支付");
        order.setPaymentNo("PAY1");
        order.setPaymentExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(orderMapper.selectByPaymentNoForUpdate("PAY1"))
                .thenReturn(List.of(order));

        var payment = cartService.renewPayment(principal, "PAY1");

        assertEquals("PENDING", payment.status());
        verify(orderMapper).updateById(order);
    }

    @Test
    void renewPaymentRejectsAnotherUsersPayment() {
        AuthPrincipal principal = new AuthPrincipal("u1", "user@demo.com", "USER", null);
        ShopOrder order = new ShopOrder();
        order.setUserId("u2");
        order.setStatus("待支付");

        when(orderMapper.selectByPaymentNoForUpdate("PAY1"))
                .thenReturn(List.of(order));

        assertThrows(RuntimeException.class, () -> cartService.renewPayment(principal, "PAY1"));

        verify(orderMapper, never()).updateById(any(ShopOrder.class));
    }
}
