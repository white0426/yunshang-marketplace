package com.yunshang.market.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yunshang.market.common.ApiException;
import com.yunshang.market.dto.ApiDtos.ReviewRequest;
import com.yunshang.market.entity.OrderItem;
import com.yunshang.market.entity.Review;
import com.yunshang.market.entity.ShopOrder;
import com.yunshang.market.mapper.OrderItemMapper;
import com.yunshang.market.mapper.ReviewMapper;
import com.yunshang.market.mapper.ShopOrderMapper;
import com.yunshang.market.security.AuthPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReviewService {
    private final ReviewMapper reviewMapper;
    private final ShopOrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    public ReviewService(
            ReviewMapper reviewMapper,
            ShopOrderMapper orderMapper,
            OrderItemMapper orderItemMapper
    ) {
        this.reviewMapper = reviewMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
    }

    @Transactional
    public void reviewProduct(AuthPrincipal principal, String productId, ReviewRequest request) {
        Long existing = reviewMapper.selectCount(Wrappers.<Review>lambdaQuery()
                .eq(Review::getUserId, principal.userId())
                .eq(Review::getProductId, productId)
                .eq(Review::getReviewType, "PRODUCT"));
        if (existing > 0) {
            throw ApiException.badRequest("你已经评价过这件商品");
        }

        List<ShopOrder> completedOrders = completedOrders(principal);
        ShopOrder matchedOrder = null;
        for (ShopOrder order : completedOrders) {
            List<OrderItem> items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                    .eq(OrderItem::getOrderId, order.getId())
                    .eq(OrderItem::getProductId, productId));
            if (!items.isEmpty()) {
                matchedOrder = order;
                break;
            }
        }
        if (matchedOrder == null) {
            throw ApiException.badRequest("完成购买后才能评价该商品");
        }

        Review review = new Review();
        review.setUserId(principal.userId());
        review.setMerchantId(matchedOrder.getMerchantId());
        review.setProductId(productId);
        review.setOrderId(matchedOrder.getId());
        review.setReviewType("PRODUCT");
        review.setRating(request.rating());
        review.setContent(request.content().trim());
        review.setVerifiedPurchase(true);
        reviewMapper.insert(review);
    }

    @Transactional
    public void reviewMerchant(
            AuthPrincipal principal,
            String merchantId,
            ReviewRequest request
    ) {
        Long existing = reviewMapper.selectCount(Wrappers.<Review>lambdaQuery()
                .eq(Review::getUserId, principal.userId())
                .eq(Review::getMerchantId, merchantId)
                .eq(Review::getReviewType, "MERCHANT"));
        if (existing > 0) {
            throw ApiException.badRequest("你已经评价过这家店铺");
        }

        ShopOrder matchedOrder = completedOrders(principal).stream()
                .filter(order -> merchantId.equals(order.getMerchantId()))
                .findFirst()
                .orElseThrow(() -> ApiException.badRequest("完成购买后才能评价这家店铺"));

        Review review = new Review();
        review.setUserId(principal.userId());
        review.setMerchantId(merchantId);
        review.setOrderId(matchedOrder.getId());
        review.setReviewType("MERCHANT");
        review.setRating(request.rating());
        review.setContent(request.content().trim());
        review.setVerifiedPurchase(true);
        reviewMapper.insert(review);
    }

    private List<ShopOrder> completedOrders(AuthPrincipal principal) {
        return orderMapper.selectList(Wrappers.<ShopOrder>lambdaQuery()
                .eq(ShopOrder::getUserId, principal.userId())
                .eq(ShopOrder::getStatus, "已完成")
                .orderByDesc(ShopOrder::getCreatedAt));
    }
}
