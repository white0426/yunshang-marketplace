package com.yunshang.market.service;

import com.yunshang.market.common.ApiException;
import com.yunshang.market.dto.ApiDtos.ReviewRequest;
import com.yunshang.market.entity.OrderItem;
import com.yunshang.market.entity.Review;
import com.yunshang.market.entity.ShopOrder;
import com.yunshang.market.mapper.OrderItemMapper;
import com.yunshang.market.mapper.ReviewMapper;
import com.yunshang.market.mapper.ShopOrderMapper;
import com.yunshang.market.security.AuthPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {
    @Mock
    private ReviewMapper reviewMapper;
    @Mock
    private ShopOrderMapper orderMapper;
    @Mock
    private OrderItemMapper orderItemMapper;
    @InjectMocks
    private ReviewService reviewService;

    private final AuthPrincipal principal =
            new AuthPrincipal("u1", "user@demo.com", "USER", null);

    @Test
    void completedPurchaseCanReviewProduct() {
        AtomicReference<Review> inserted = new AtomicReference<>();
        ShopOrder order = new ShopOrder();
        order.setId("o1");
        order.setUserId("u1");
        order.setMerchantId("m1");
        order.setStatus("已完成");
        OrderItem item = new OrderItem();
        item.setOrderId("o1");
        item.setProductId("p1");

        when(reviewMapper.selectCount(any())).thenReturn(0L);
        when(orderMapper.selectList(any())).thenReturn(List.of(order));
        when(orderItemMapper.selectList(any())).thenReturn(List.of(item));
        when(reviewMapper.insert(any(Review.class))).thenAnswer(invocation -> {
            inserted.set(invocation.getArgument(0));
            return 1;
        });

        reviewService.reviewProduct(principal, "p1", new ReviewRequest(5, "很好用"));

        assertEquals("PRODUCT", inserted.get().getReviewType());
        assertEquals("o1", inserted.get().getOrderId());
        assertEquals(true, inserted.get().getVerifiedPurchase());
    }

    @Test
    void productReviewRequiresCompletedPurchase() {
        when(reviewMapper.selectCount(any())).thenReturn(0L);
        when(orderMapper.selectList(any())).thenReturn(List.of());

        assertThrows(
                ApiException.class,
                () -> reviewService.reviewProduct(
                        principal,
                        "p1",
                        new ReviewRequest(5, "很好用")
                )
        );

        verify(reviewMapper, never()).insert(any(Review.class));
    }

    @Test
    void merchantCanOnlyBeReviewedOnce() {
        when(reviewMapper.selectCount(any())).thenReturn(1L);

        assertThrows(
                ApiException.class,
                () -> reviewService.reviewMerchant(
                        principal,
                        "m1",
                        new ReviewRequest(5, "店铺不错")
                )
        );

        verify(orderMapper, never()).selectList(any());
        verify(reviewMapper, never()).insert(any(Review.class));
    }
}
