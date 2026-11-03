package com.yunshang.market.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yunshang.market.common.ApiException;
import com.yunshang.market.dto.ApiDtos.CheckoutRequest;
import com.yunshang.market.dto.ApiDtos.PaymentView;
import com.yunshang.market.entity.CartItem;
import com.yunshang.market.entity.Merchant;
import com.yunshang.market.entity.MerchantDailyStat;
import com.yunshang.market.entity.OrderItem;
import com.yunshang.market.entity.Product;
import com.yunshang.market.entity.ShopOrder;
import com.yunshang.market.mapper.CartItemMapper;
import com.yunshang.market.mapper.MerchantDailyStatMapper;
import com.yunshang.market.mapper.MerchantMapper;
import com.yunshang.market.mapper.OrderItemMapper;
import com.yunshang.market.mapper.ProductMapper;
import com.yunshang.market.mapper.ShopOrderMapper;
import com.yunshang.market.security.AuthPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CartService {
    private static final String PAYMENT_PENDING = "待支付";
    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final CartItemMapper cartMapper;
    private final ProductMapper productMapper;
    private final MerchantMapper merchantMapper;
    private final ShopOrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final MerchantDailyStatMapper statMapper;

    public CartService(
            CartItemMapper cartMapper,
            ProductMapper productMapper,
            MerchantMapper merchantMapper,
            ShopOrderMapper orderMapper,
            OrderItemMapper orderItemMapper,
            MerchantDailyStatMapper statMapper
    ) {
        this.cartMapper = cartMapper;
        this.productMapper = productMapper;
        this.merchantMapper = merchantMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.statMapper = statMapper;
    }

    @Transactional
    public void add(AuthPrincipal principal, String productId, int quantity) {
        Product product = requirePurchasableProduct(productId);
        CartItem existing = cartMapper.selectOne(Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getUserId, principal.userId())
                .eq(CartItem::getProductId, productId)
                .last("LIMIT 1"));
        int nextQuantity = (existing == null ? 0 : existing.getQuantity()) + quantity;
        if (nextQuantity > product.getStock()) {
            throw ApiException.badRequest("商品库存不足");
        }
        if (existing == null) {
            CartItem item = new CartItem();
            item.setUserId(principal.userId());
            item.setProductId(productId);
            item.setQuantity(quantity);
            cartMapper.insert(item);
        } else {
            existing.setQuantity(nextQuantity);
            cartMapper.updateById(existing);
        }
    }

    public void update(AuthPrincipal principal, String productId, int quantity) {
        Product product = requirePurchasableProduct(productId);
        if (quantity > product.getStock()) {
            throw ApiException.badRequest("商品库存不足");
        }
        CartItem item = cartMapper.selectOne(Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getUserId, principal.userId())
                .eq(CartItem::getProductId, productId)
                .last("LIMIT 1"));
        if (item == null) {
            throw ApiException.notFound("购物车中没有该商品");
        }
        item.setQuantity(quantity);
        cartMapper.updateById(item);
    }

    public void remove(AuthPrincipal principal, String productId) {
        cartMapper.delete(Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getUserId, principal.userId())
                .eq(CartItem::getProductId, productId));
    }

    @Transactional
    public PaymentView checkout(AuthPrincipal principal, CheckoutRequest receiver) {
        List<CartItem> cartItems = cartMapper.selectList(Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getUserId, principal.userId())
                .orderByAsc(CartItem::getId));
        if (cartItems.isEmpty()) {
            throw ApiException.badRequest("购物车还是空的");
        }

        Map<String, Product> products = new LinkedHashMap<>();
        for (CartItem item : cartItems) {
            Product product = requirePurchasableProduct(item.getProductId());
            if (item.getQuantity() > product.getStock()) {
                throw ApiException.badRequest(product.getName() + " 库存不足");
            }
            products.put(item.getProductId(), product);
        }

        Map<String, List<CartItem>> groups = cartItems.stream()
                .collect(Collectors.groupingBy(
                        item -> products.get(item.getProductId()).getMerchantId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<String> orderIds = new ArrayList<>();
        String paymentNo = createPaymentNo();
        LocalDateTime createdAt = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        LocalDateTime paymentExpiresAt = createdAt.plusMinutes(20);
        BigDecimal paymentAmount = BigDecimal.ZERO;
        for (Map.Entry<String, List<CartItem>> entry : groups.entrySet()) {
            String merchantId = entry.getKey();
            List<CartItem> lines = entry.getValue();
            BigDecimal total = lines.stream()
                    .map(item -> products.get(item.getProductId()).getPrice()
                            .multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            ShopOrder order = new ShopOrder();
            order.setId("YS" + UUID.randomUUID().toString().replace("-", "").substring(0, 20));
            order.setUserId(principal.userId());
            order.setMerchantId(merchantId);
            order.setTotal(total);
            order.setStatus(PAYMENT_PENDING);
            order.setPaymentNo(paymentNo);
            order.setPaymentExpiresAt(paymentExpiresAt);
            order.setReceiverName(receiver.receiverName());
            order.setReceiverPhone(receiver.receiverPhone());
            order.setReceiverAddress(receiver.receiverAddress());
            order.setCreatedAt(createdAt);
            orderMapper.insert(order);
            orderIds.add(order.getId());
            paymentAmount = paymentAmount.add(total);

            for (CartItem line : lines) {
                Product product = products.get(line.getProductId());
                OrderItem orderItem = new OrderItem();
                orderItem.setOrderId(order.getId());
                orderItem.setProductId(product.getId());
                orderItem.setQuantity(line.getQuantity());
                orderItem.setPrice(product.getPrice());
                orderItemMapper.insert(orderItem);

                if (productMapper.deductStock(product.getId(), line.getQuantity()) == 0) {
                    throw ApiException.badRequest(product.getName() + " 库存不足，请刷新后重试");
                }
            }

        }

        cartMapper.delete(Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getUserId, principal.userId()));
        return new PaymentView(
                paymentNo,
                orderIds,
                paymentAmount,
                "PENDING",
                paymentExpiresAt.format(DATE_TIME_FORMAT),
                null
        );
    }

    public PaymentView payment(String paymentNo) {
        List<ShopOrder> orders = orderMapper.selectList(Wrappers.<ShopOrder>lambdaQuery()
                .eq(ShopOrder::getPaymentNo, paymentNo)
                .orderByAsc(ShopOrder::getCreatedAt));
        if (orders.isEmpty()) {
            throw ApiException.notFound("支付单不存在");
        }
        return toPaymentView(orders);
    }

    @Transactional
    public PaymentView pay(String paymentNo) {
        List<ShopOrder> orders = orderMapper.selectByPaymentNoForUpdate(paymentNo);
        if (orders.isEmpty()) {
            throw ApiException.notFound("支付单不存在");
        }

        boolean alreadyPaid = orders.stream()
                .allMatch(order -> !PAYMENT_PENDING.equals(order.getStatus()));
        if (alreadyPaid) {
            return toPaymentView(orders);
        }

        ShopOrder first = orders.get(0);
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        if (first.getPaymentExpiresAt() == null || !now.isBefore(first.getPaymentExpiresAt())) {
            throw ApiException.badRequest("支付二维码已过期，请返回订单重新发起支付");
        }

        for (ShopOrder order : orders) {
            if (!PAYMENT_PENDING.equals(order.getStatus())) {
                throw ApiException.badRequest("支付单状态异常，请刷新后重试");
            }
            order.setStatus("待发货");
            order.setPaidAt(now);
            orderMapper.updateById(order);
            statMapper.addOrderStat(
                    order.getMerchantId(),
                    now.toLocalDate(),
                    order.getTotal()
            );
        }
        return toPaymentView(orders);
    }

    @Transactional
    public PaymentView renewPayment(AuthPrincipal principal, String paymentNo) {
        List<ShopOrder> orders = orderMapper.selectByPaymentNoForUpdate(paymentNo);
        if (orders.isEmpty()) {
            throw ApiException.notFound("支付单不存在");
        }
        if (orders.stream().anyMatch(order -> !principal.userId().equals(order.getUserId()))) {
            throw ApiException.forbidden("不能操作其他用户的支付单");
        }
        if (orders.stream().noneMatch(order -> PAYMENT_PENDING.equals(order.getStatus()))) {
            throw ApiException.badRequest("当前支付单已完成，无需重新支付");
        }

        LocalDateTime expiresAt = LocalDateTime.now(ZoneId.of("Asia/Shanghai"))
                .plusMinutes(20);
        for (ShopOrder order : orders) {
            if (!PAYMENT_PENDING.equals(order.getStatus())) {
                throw ApiException.badRequest("支付单状态异常，请刷新后重试");
            }
            order.setPaymentExpiresAt(expiresAt);
            orderMapper.updateById(order);
        }
        return toPaymentView(orders);
    }

    @Transactional
    public void cancelOrder(AuthPrincipal principal, String orderId) {
        ShopOrder order = requireUserOrder(principal, orderId);
        boolean unpaid = PAYMENT_PENDING.equals(order.getStatus());
        if (!unpaid && !"待发货".equals(order.getStatus())) {
            throw ApiException.badRequest("当前订单状态不允许取消");
        }

        List<OrderItem> items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderId, orderId));
        for (OrderItem item : items) {
            if (productMapper.restoreStock(item.getProductId(), item.getQuantity()) == 0) {
                throw ApiException.badRequest("订单商品不存在，无法取消订单");
            }
        }

        if (!unpaid) {
            statMapper.subtractOrderStat(
                    order.getMerchantId(),
                    order.getCreatedAt().toLocalDate(),
                    order.getTotal()
            );
        }
        order.setStatus("已取消");
        orderMapper.updateById(order);
    }

    public void confirmReceipt(AuthPrincipal principal, String orderId) {
        updateUserOrderStatus(principal, orderId, "待收货", "已完成");
    }

    private void updateUserOrderStatus(
            AuthPrincipal principal,
            String orderId,
            String expectedStatus,
            String nextStatus
    ) {
        ShopOrder order = requireUserOrder(principal, orderId);
        if (!expectedStatus.equals(order.getStatus())) {
            throw ApiException.badRequest("当前订单状态不允许该操作");
        }
        order.setStatus(nextStatus);
        orderMapper.updateById(order);
    }

    private ShopOrder requireUserOrder(AuthPrincipal principal, String orderId) {
        ShopOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw ApiException.notFound("订单不存在");
        }
        if (!principal.userId().equals(order.getUserId())) {
            throw ApiException.forbidden("不能操作其他用户的订单");
        }
        return order;
    }

    private Product requirePurchasableProduct(String productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw ApiException.notFound("商品不存在");
        }
        if (!"ONLINE".equals(product.getStatus())) {
            throw ApiException.badRequest("商品已下架");
        }
        Merchant merchant = merchantMapper.selectById(product.getMerchantId());
        if (merchant == null || !"ACTIVE".equals(merchant.getStatus())) {
            throw ApiException.badRequest("商家已暂停营业");
        }
        return product;
    }

    private PaymentView toPaymentView(List<ShopOrder> orders) {
        List<String> orderIds = orders.stream().map(ShopOrder::getId).toList();
        BigDecimal amount = orders.stream()
                .map(ShopOrder::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        ShopOrder first = orders.get(0);
        boolean pending = orders.stream()
                .anyMatch(order -> PAYMENT_PENDING.equals(order.getStatus()));
        boolean expired = pending
                && first.getPaymentExpiresAt() != null
                && !LocalDateTime.now(ZoneId.of("Asia/Shanghai"))
                .isBefore(first.getPaymentExpiresAt());
        String status = pending ? (expired ? "EXPIRED" : "PENDING") : "PAID";
        LocalDateTime paidAt = orders.stream()
                .map(ShopOrder::getPaidAt)
                .filter(value -> value != null)
                .findFirst()
                .orElse(null);
        return new PaymentView(
                first.getPaymentNo(),
                orderIds,
                amount,
                status,
                first.getPaymentExpiresAt() == null
                        ? null
                        : first.getPaymentExpiresAt().format(DATE_TIME_FORMAT),
                paidAt == null ? null : paidAt.format(DATE_TIME_FORMAT)
        );
    }

    private String createPaymentNo() {
        return "PAY"
                + LocalDateTime.now(ZoneId.of("Asia/Shanghai"))
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

}
