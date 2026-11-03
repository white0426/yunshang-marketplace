package com.yunshang.market.service;

import com.yunshang.market.common.ApiException;
import com.yunshang.market.dto.ApiDtos.CreateProductRequest;
import com.yunshang.market.dto.ApiDtos.MerchantProfileRequest;
import com.yunshang.market.dto.ApiDtos.ProductUpdateRequest;
import com.yunshang.market.dto.ApiDtos.ShipOrderRequest;
import com.yunshang.market.entity.Merchant;
import com.yunshang.market.entity.Product;
import com.yunshang.market.entity.ShopOrder;
import com.yunshang.market.mapper.MerchantMapper;
import com.yunshang.market.mapper.ProductMapper;
import com.yunshang.market.mapper.ShopOrderMapper;
import com.yunshang.market.security.AuthPrincipal;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.Map;

@Service
public class MerchantService {
    private static final String TRACKING_ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final SecureRandom TRACKING_RANDOM = new SecureRandom();
    private static final Map<String, String> COURIER_CODES = Map.of(
            "顺丰速运", "SF",
            "圆通速递", "YT",
            "中通快递", "ZT",
            "韵达快递", "YD",
            "京东物流", "JD",
            "邮政快递", "YZ"
    );

    private final MerchantMapper merchantMapper;
    private final ProductMapper productMapper;
    private final ShopOrderMapper orderMapper;

    public MerchantService(
            MerchantMapper merchantMapper,
            ProductMapper productMapper,
            ShopOrderMapper orderMapper
    ) {
        this.merchantMapper = merchantMapper;
        this.productMapper = productMapper;
        this.orderMapper = orderMapper;
    }

    public Product createProduct(AuthPrincipal principal, CreateProductRequest request) {
        String merchantId = requireMerchantId(principal);
        Product product = new Product();
        product.setId("p" + System.currentTimeMillis());
        product.setMerchantId(merchantId);
        product.setName(request.name().trim());
        product.setCategory(request.category());
        product.setPrice(request.price());
        product.setOriginalPrice(
                request.originalPrice() == null ? request.price() : request.originalPrice()
        );
        product.setStock(request.stock());
        product.setSold(0);
        product.setStatus("offline".equalsIgnoreCase(request.status()) ? "OFFLINE" : "ONLINE");
        product.setImage(
                request.image() == null || request.image().isBlank()
                        ? "/images/chair.jpg"
                        : request.image()
        );
        product.setDescription(
                request.description() == null || request.description().isBlank()
                        ? "商家正在完善商品介绍。"
                        : request.description().trim()
        );
        product.setTags(
                request.tags() == null || request.tags().isBlank()
                        ? "新品"
                        : request.tags().trim()
        );
        product.setCreatedAt(LocalDate.now());
        productMapper.insert(product);
        return product;
    }

    public void updatePrice(AuthPrincipal principal, String productId, BigDecimal price) {
        Product product = requireOwnedProduct(principal, productId);
        product.setPrice(price);
        productMapper.updateById(product);
    }

    public void updateProductStatus(AuthPrincipal principal, String productId, String status) {
        Product product = requireOwnedProduct(principal, productId);
        product.setStatus(normalizeOnlineStatus(status));
        productMapper.updateById(product);
    }

    public void updateProduct(
            AuthPrincipal principal,
            String productId,
            ProductUpdateRequest request
    ) {
        Product product = requireOwnedProduct(principal, productId);
        product.setStock(request.stock());
        product.setCategory(request.category());
        if (request.image() != null && !request.image().isBlank()) {
            product.setImage(request.image());
        }
        product.setDescription(request.description());
        productMapper.updateById(product);
    }

    public void updateProfile(AuthPrincipal principal, MerchantProfileRequest request) {
        String merchantId = requireMerchantId(principal);
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw ApiException.notFound("商家不存在");
        }
        merchant.setName(request.name().trim());
        merchant.setOwnerName(request.owner().trim());
        merchant.setPhone(request.phone());
        merchant.setLocation(request.location());
        merchant.setDescription(request.description());
        merchantMapper.updateById(merchant);
    }

    public void updateOrderStatus(AuthPrincipal principal, String orderId, String status) {
        ShopOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw ApiException.notFound("订单不存在");
        }
        if (!requireMerchantId(principal).equals(order.getMerchantId())) {
            throw ApiException.forbidden("不能处理其他商家的订单");
        }
        if (!"待收货".equals(status)) {
            throw ApiException.badRequest("商家只能执行确认发货操作");
        }
        if (!"待发货".equals(order.getStatus())) {
            throw ApiException.badRequest("订单只能从待发货更新为待收货，完成后由用户确认收货");
        }
        order.setStatus(status);
        orderMapper.updateById(order);
    }

    public String shipOrder(
            AuthPrincipal principal,
            String orderId,
            ShipOrderRequest request
    ) {
        ShopOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw ApiException.notFound("订单不存在");
        }
        if (!requireMerchantId(principal).equals(order.getMerchantId())) {
            throw ApiException.forbidden("不能处理其他商家的订单");
        }
        if (!"待发货".equals(order.getStatus())) {
            throw ApiException.badRequest("只有待发货订单可以确认发货");
        }
        String shippingCompany = request.shippingCompany().trim();
        String courierCode = COURIER_CODES.get(shippingCompany);
        if (courierCode == null) {
            throw ApiException.badRequest("物流公司不正确");
        }
        String trackingNo = generateTrackingNo(courierCode);
        order.setStatus("待收货");
        order.setShippingCompany(shippingCompany);
        order.setTrackingNo(trackingNo);
        order.setShippedAt(LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
        orderMapper.updateById(order);
        return trackingNo;
    }

    private String generateTrackingNo(String courierCode) {
        StringBuilder value = new StringBuilder(24).append(courierCode);
        while (value.length() < 24) {
            value.append(TRACKING_ALPHABET.charAt(
                    TRACKING_RANDOM.nextInt(TRACKING_ALPHABET.length())
            ));
        }
        return value.toString().toUpperCase(Locale.ROOT);
    }

    private Product requireOwnedProduct(AuthPrincipal principal, String productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw ApiException.notFound("商品不存在");
        }
        if (!requireMerchantId(principal).equals(product.getMerchantId())) {
            throw ApiException.forbidden("不能修改其他商家的商品");
        }
        return product;
    }

    private String requireMerchantId(AuthPrincipal principal) {
        if (principal.merchantId() == null || principal.merchantId().isBlank()) {
            throw ApiException.forbidden("当前账号未绑定商家");
        }
        return principal.merchantId();
    }

    private String normalizeOnlineStatus(String status) {
        if ("online".equalsIgnoreCase(status) || "ONLINE".equalsIgnoreCase(status)) {
            return "ONLINE";
        }
        if ("offline".equalsIgnoreCase(status) || "OFFLINE".equalsIgnoreCase(status)) {
            return "OFFLINE";
        }
        throw ApiException.badRequest("商品状态不正确");
    }
}
