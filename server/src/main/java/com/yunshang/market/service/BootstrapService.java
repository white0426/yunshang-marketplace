package com.yunshang.market.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yunshang.market.common.ApiException;
import com.yunshang.market.dto.ApiDtos.AccountView;
import com.yunshang.market.dto.ApiDtos.AddressView;
import com.yunshang.market.dto.ApiDtos.AfterSaleView;
import com.yunshang.market.dto.ApiDtos.BootstrapView;
import com.yunshang.market.dto.ApiDtos.CartLine;
import com.yunshang.market.dto.ApiDtos.MerchantView;
import com.yunshang.market.dto.ApiDtos.OrderItemView;
import com.yunshang.market.dto.ApiDtos.OrderView;
import com.yunshang.market.dto.ApiDtos.OperationLogView;
import com.yunshang.market.dto.ApiDtos.ProductView;
import com.yunshang.market.dto.ApiDtos.PublicMerchantView;
import com.yunshang.market.dto.ApiDtos.ReviewView;
import com.yunshang.market.dto.ApiDtos.StorefrontView;
import com.yunshang.market.dto.ApiDtos.UserView;
import com.yunshang.market.entity.CartItem;
import com.yunshang.market.entity.Merchant;
import com.yunshang.market.entity.MerchantDailyStat;
import com.yunshang.market.entity.OrderItem;
import com.yunshang.market.entity.Product;
import com.yunshang.market.entity.Review;
import com.yunshang.market.entity.ShopOrder;
import com.yunshang.market.entity.SysUser;
import com.yunshang.market.entity.UserAddress;
import com.yunshang.market.mapper.CartItemMapper;
import com.yunshang.market.mapper.MerchantDailyStatMapper;
import com.yunshang.market.mapper.MerchantMapper;
import com.yunshang.market.mapper.OrderItemMapper;
import com.yunshang.market.mapper.ProductMapper;
import com.yunshang.market.mapper.ReviewMapper;
import com.yunshang.market.mapper.ShopOrderMapper;
import com.yunshang.market.mapper.SysUserMapper;
import com.yunshang.market.mapper.UserAddressMapper;
import com.yunshang.market.security.AuthPrincipal;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BootstrapService {
    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final MerchantMapper merchantMapper;
    private final SysUserMapper userMapper;
    private final ProductMapper productMapper;
    private final CartItemMapper cartItemMapper;
    private final ShopOrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final MerchantDailyStatMapper statMapper;
    private final UserAddressMapper addressMapper;
    private final ReviewMapper reviewMapper;
    private final AfterSaleService afterSaleService;
    private final AuditLogService auditLogService;

    public BootstrapService(
            MerchantMapper merchantMapper,
            SysUserMapper userMapper,
            ProductMapper productMapper,
            CartItemMapper cartItemMapper,
            ShopOrderMapper orderMapper,
            OrderItemMapper orderItemMapper,
            MerchantDailyStatMapper statMapper,
            UserAddressMapper addressMapper,
            ReviewMapper reviewMapper,
            AfterSaleService afterSaleService,
            AuditLogService auditLogService
    ) {
        this.merchantMapper = merchantMapper;
        this.userMapper = userMapper;
        this.productMapper = productMapper;
        this.cartItemMapper = cartItemMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.statMapper = statMapper;
        this.addressMapper = addressMapper;
        this.reviewMapper = reviewMapper;
        this.afterSaleService = afterSaleService;
        this.auditLogService = auditLogService;
    }

    public BootstrapView load(AuthPrincipal principal) {
        SysUser current = userMapper.selectById(principal.userId());
        if (current == null || !"ACTIVE".equals(current.getStatus())) {
            throw ApiException.unauthorized("登录账号已失效，请重新登录");
        }

        List<Merchant> merchants = loadMerchants(current);
        Set<String> merchantIds = merchants.stream()
                .map(Merchant::getId)
                .collect(Collectors.toSet());

        List<Product> products = loadProducts(current, merchantIds);
        List<ShopOrder> orders = loadOrders(current);
        Map<String, List<OrderItem>> orderItems = loadOrderItems(orders);
        List<SysUser> users = loadVisibleUsers(current, orders);

        Map<String, List<MerchantDailyStat>> statsByMerchant =
                statMapper.selectList(Wrappers.<MerchantDailyStat>lambdaQuery()
                                .in(MerchantDailyStat::getMerchantId, merchantIds)
                                .ge(MerchantDailyStat::getStatDate,
                                        LocalDate.now(ZoneId.of("Asia/Shanghai")).minusDays(6))
                                .orderByAsc(MerchantDailyStat::getStatDate))
                        .stream()
                        .collect(Collectors.groupingBy(MerchantDailyStat::getMerchantId));

        List<MerchantView> merchantViews = merchants.stream()
                .map(merchant -> toMerchantView(
                        merchant,
                        statsByMerchant.getOrDefault(merchant.getId(), List.of())
                ))
                .toList();

        List<ProductView> productViews = products.stream()
                .map(this::toProductView)
                .toList();

        List<UserView> userViews = users.stream()
                .sorted(Comparator.comparing(SysUser::getJoinedAt).reversed())
                .map(this::toUserView)
                .toList();

        List<CartLine> cart = current.getRole().equals("USER")
                ? cartItemMapper.selectList(Wrappers.<CartItem>lambdaQuery()
                                .eq(CartItem::getUserId, current.getId())
                                .orderByAsc(CartItem::getId))
                        .stream()
                        .map(item -> new CartLine(item.getProductId(), item.getQuantity()))
                        .toList()
                : List.of();
        List<AddressView> addresses = current.getRole().equals("USER")
                ? addressMapper.selectByUser(current.getId())
                        .stream()
                        .map(address -> new AddressView(
                                address.getId(),
                                address.getReceiverName(),
                                address.getReceiverPhone(),
                                address.getRegion(),
                                address.getDetail(),
                                address.getRegion() + " " + address.getDetail(),
                                Boolean.TRUE.equals(address.getIsDefault())
                        ))
                        .toList()
                : List.of();
        List<ReviewView> reviews = loadReviews();
        List<AfterSaleView> afterSales = afterSaleService.list(principal);
        List<OperationLogView> operationLogs = "ADMIN".equals(principal.role())
                ? auditLogService.list()
                : List.of();

        List<OrderView> orderViews = orders.stream()
                .map(order -> new OrderView(
                        order.getId(),
                        order.getUserId(),
                        order.getMerchantId(),
                        orderItems.getOrDefault(order.getId(), List.of()).stream()
                                .map(item -> new OrderItemView(
                                        item.getProductId(),
                                        item.getQuantity(),
                                        item.getPrice()
                                ))
                                .toList(),
                        order.getTotal(),
                        order.getStatus(),
                        order.getPaymentNo(),
                        order.getPaymentExpiresAt() == null
                                ? null
                                : order.getPaymentExpiresAt().format(DATE_TIME_FORMAT),
                        order.getShippingCompany(),
                        order.getTrackingNo(),
                        order.getShippedAt() == null
                                ? null
                                : order.getShippedAt().format(DATE_TIME_FORMAT),
                        order.getReceiverName(),
                        order.getReceiverPhone(),
                        order.getReceiverAddress(),
                        order.getCreatedAt().format(DATE_TIME_FORMAT)
                ))
                .toList();

        return new BootstrapView(
                current.getRole().toLowerCase(),
                toAccountView(current),
                merchantViews,
                productViews,
                userViews,
                cart,
                addresses,
                reviews,
                afterSales,
                operationLogs,
                orderViews
        );
    }

    public AccountView accountView(SysUser user) {
        return toAccountView(user);
    }

    public StorefrontView loadStorefront() {
        List<Merchant> merchants = merchantMapper.selectList(Wrappers.<Merchant>lambdaQuery()
                .eq(Merchant::getStatus, "ACTIVE")
                .orderByAsc(Merchant::getJoinedAt));
        Set<String> merchantIds = merchants.stream()
                .map(Merchant::getId)
                .collect(Collectors.toSet());
        List<Product> products = merchantIds.isEmpty()
                ? List.of()
                : productMapper.selectList(Wrappers.<Product>lambdaQuery()
                        .eq(Product::getStatus, "ONLINE")
                        .in(Product::getMerchantId, merchantIds)
                        .orderByDesc(Product::getSold));
        return new StorefrontView(
                merchants.stream().map(this::toPublicMerchantView).toList(),
                products.stream().map(this::toProductView).toList(),
                loadReviews()
        );
    }

    private List<ReviewView> loadReviews() {
        List<Review> reviews = reviewMapper.selectList(Wrappers.<Review>lambdaQuery()
                .orderByDesc(Review::getCreatedAt)
                .orderByDesc(Review::getId));
        if (reviews.isEmpty()) {
            return List.of();
        }
        Map<String, SysUser> users = userMapper.selectBatchIds(
                        reviews.stream().map(Review::getUserId).distinct().toList()
                )
                .stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        return reviews.stream()
                .map(review -> {
                    SysUser reviewer = users.get(review.getUserId());
                    return new ReviewView(
                            review.getId(),
                            review.getUserId(),
                            reviewer == null ? "匿名用户" : reviewer.getDisplayName(),
                            reviewer == null ? "匿" : reviewer.getAvatar(),
                            review.getMerchantId(),
                            review.getProductId(),
                            review.getReviewType(),
                            review.getRating(),
                            review.getContent(),
                            Boolean.TRUE.equals(review.getVerifiedPurchase()),
                            review.getCreatedAt().format(DATE_TIME_FORMAT)
                    );
                })
                .toList();
    }

    private List<Merchant> loadMerchants(SysUser current) {
        if ("ADMIN".equals(current.getRole())) {
            return merchantMapper.selectList(Wrappers.<Merchant>lambdaQuery()
                    .orderByAsc(Merchant::getJoinedAt));
        }
        if ("MERCHANT".equals(current.getRole())) {
            Merchant merchant = merchantMapper.selectById(current.getMerchantId());
            return merchant == null ? List.of() : List.of(merchant);
        }
        return merchantMapper.selectList(Wrappers.<Merchant>lambdaQuery()
                .eq(Merchant::getStatus, "ACTIVE")
                .orderByAsc(Merchant::getJoinedAt));
    }

    private List<Product> loadProducts(SysUser current, Set<String> merchantIds) {
        if ("ADMIN".equals(current.getRole())) {
            return productMapper.selectList(Wrappers.<Product>lambdaQuery()
                    .orderByDesc(Product::getCreatedAt));
        }
        if ("MERCHANT".equals(current.getRole())) {
            return productMapper.selectList(Wrappers.<Product>lambdaQuery()
                    .eq(Product::getMerchantId, current.getMerchantId())
                    .orderByDesc(Product::getCreatedAt));
        }
        if (merchantIds.isEmpty()) {
            return List.of();
        }
        return productMapper.selectList(Wrappers.<Product>lambdaQuery()
                .eq(Product::getStatus, "ONLINE")
                .in(Product::getMerchantId, merchantIds)
                .orderByDesc(Product::getSold));
    }

    private List<ShopOrder> loadOrders(SysUser current) {
        if ("ADMIN".equals(current.getRole())) {
            return orderMapper.selectList(Wrappers.<ShopOrder>lambdaQuery()
                    .orderByDesc(ShopOrder::getCreatedAt));
        }
        if ("MERCHANT".equals(current.getRole())) {
            return orderMapper.selectList(Wrappers.<ShopOrder>lambdaQuery()
                    .eq(ShopOrder::getMerchantId, current.getMerchantId())
                    .ne(ShopOrder::getStatus, "待支付")
                    .orderByDesc(ShopOrder::getCreatedAt));
        }
        return orderMapper.selectList(Wrappers.<ShopOrder>lambdaQuery()
                .eq(ShopOrder::getUserId, current.getId())
                .orderByDesc(ShopOrder::getCreatedAt));
    }

    private Map<String, List<OrderItem>> loadOrderItems(List<ShopOrder> orders) {
        if (orders.isEmpty()) {
            return Map.of();
        }
        List<String> orderIds = orders.stream().map(ShopOrder::getId).toList();
        return orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                        .in(OrderItem::getOrderId, orderIds)
                        .orderByAsc(OrderItem::getId))
                .stream()
                .collect(Collectors.groupingBy(
                        OrderItem::getOrderId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    private List<SysUser> loadVisibleUsers(SysUser current, List<ShopOrder> orders) {
        if ("ADMIN".equals(current.getRole())) {
            return userMapper.selectList(Wrappers.<SysUser>lambdaQuery()
                    .eq(SysUser::getRole, "USER"));
        }
        if ("MERCHANT".equals(current.getRole())) {
            Set<String> userIds = orders.stream()
                    .map(ShopOrder::getUserId)
                    .collect(Collectors.toSet());
            if (userIds.isEmpty()) {
                return List.of();
            }
            return userMapper.selectBatchIds(userIds);
        }
        return List.of(current);
    }

    private MerchantView toMerchantView(
            Merchant merchant,
            List<MerchantDailyStat> stats
    ) {
        List<LocalDate> weeklyDates = new ArrayList<>();
        List<BigDecimal> weeklySales = new ArrayList<>();
        List<Integer> weeklyOrders = new ArrayList<>();
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        LocalDate start = today.minusDays(6);
        Map<LocalDate, MerchantDailyStat> statsByDate = new HashMap<>();
        for (MerchantDailyStat stat : stats) {
            statsByDate.put(stat.getStatDate(), stat);
        }
        for (int offset = 0; offset < 7; offset++) {
            LocalDate date = start.plusDays(offset);
            MerchantDailyStat stat = statsByDate.get(date);
            weeklyDates.add(date);
            weeklySales.add(stat == null ? BigDecimal.ZERO : stat.getSalesAmount());
            weeklyOrders.add(stat == null ? 0 : stat.getOrderCount());
        }
        return new MerchantView(
                merchant.getId(),
                merchant.getName(),
                merchant.getOwnerName(),
                merchant.getEmail(),
                merchant.getPhone(),
                merchant.getLocation(),
                merchant.getDescription(),
                merchant.getBanner(),
                merchant.getAccent(),
                merchant.getStatus().toLowerCase(),
                merchant.getReviewNote(),
                merchant.getReviewedAt() == null
                        ? null
                        : merchant.getReviewedAt().format(DATE_TIME_FORMAT),
                merchant.getRating(),
                merchant.getJoinedAt(),
                weeklyDates,
                weeklySales,
                weeklyOrders
        );
    }

    private ProductView toProductView(Product product) {
        List<String> tags = product.getTags() == null || product.getTags().isBlank()
                ? List.of()
                : List.of(product.getTags().split(","));
        return new ProductView(
                product.getId(),
                product.getMerchantId(),
                product.getName(),
                product.getCategory(),
                product.getPrice(),
                product.getOriginalPrice(),
                product.getStock(),
                product.getSold(),
                product.getStatus().toLowerCase(),
                product.getImage(),
                product.getDescription(),
                tags,
                product.getCreatedAt()
        );
    }

    private PublicMerchantView toPublicMerchantView(Merchant merchant) {
        return new PublicMerchantView(
                merchant.getId(),
                merchant.getName(),
                merchant.getLocation(),
                merchant.getDescription(),
                merchant.getBanner(),
                merchant.getAccent(),
                merchant.getStatus().toLowerCase(),
                merchant.getRating(),
                merchant.getJoinedAt()
        );
    }

    private UserView toUserView(SysUser user) {
        return new UserView(
                user.getId(),
                user.getDisplayName(),
                user.getEmail(),
                user.getPhone(),
                user.getStatus().toLowerCase(),
                user.getMemberLevel(),
                user.getJoinedAt(),
                user.getCity(),
                user.getAvatar()
        );
    }

    private AccountView toAccountView(SysUser user) {
        return new AccountView(
                user.getId(),
                user.getDisplayName(),
                user.getEmail(),
                user.getRole().toLowerCase(),
                user.getAvatar(),
                user.getMerchantId(),
                user.getMemberLevel(),
                user.getCity(),
                user.getPhone()
        );
    }
}
