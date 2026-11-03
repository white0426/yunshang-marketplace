package com.yunshang.market.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class ApiDtos {
    private ApiDtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "请输入账号邮箱")
            @Email(message = "邮箱格式不正确")
            String email,
            @NotBlank(message = "请输入密码")
            String password
    ) {
    }

    public record RegisterRequest(
            @NotBlank(message = "请输入姓名")
            @Size(max = 50, message = "姓名不能超过 50 个字符")
            String displayName,
            @NotBlank(message = "请输入账号邮箱")
            @Email(message = "邮箱格式不正确")
            String email,
            @NotBlank(message = "请输入手机号")
            @Size(max = 30, message = "手机号不能超过 30 个字符")
            String phone,
            @NotBlank(message = "请输入密码")
            @Size(min = 6, max = 72, message = "密码长度需为 6 至 72 个字符")
            String password
    ) {
    }

    public record MerchantRegisterRequest(
            @NotBlank(message = "请输入店铺名称")
            @Size(max = 100, message = "店铺名称不能超过 100 个字符")
            String shopName,
            @NotBlank(message = "请输入负责人姓名")
            @Size(max = 50, message = "负责人姓名不能超过 50 个字符")
            String ownerName,
            @NotBlank(message = "请输入商家邮箱")
            @Email(message = "邮箱格式不正确")
            String email,
            @NotBlank(message = "请输入联系电话")
            @Size(max = 30, message = "联系电话不能超过 30 个字符")
            String phone,
            @NotBlank(message = "请输入所在城市")
            @Size(max = 50, message = "城市不能超过 50 个字符")
            String city,
            @Size(max = 500, message = "店铺简介不能超过 500 个字符")
            String description,
            @NotBlank(message = "请输入密码")
            @Size(min = 6, max = 72, message = "密码长度需为 6 至 72 个字符")
            String password
    ) {
    }

    public record LoginResponse(
            String token,
            String role,
            String path,
            AccountView account
    ) {
    }

    public record MerchantRegisterResponse(
            Boolean pending,
            String message
    ) {
    }

    public record AccountView(
            String id,
            String name,
            String email,
            String role,
            String avatar,
            String merchantId,
            String level,
            String city,
            String phone
    ) {
    }

    public record MerchantView(
            String id,
            String name,
            String owner,
            String email,
            String phone,
            String location,
            String description,
            String banner,
            String accent,
            String status,
            String reviewNote,
            String reviewedAt,
            BigDecimal rating,
            LocalDate joinedAt,
            List<LocalDate> weeklyDates,
            List<BigDecimal> weeklySales,
            List<Integer> weeklyOrders
    ) {
    }

    public record ProductView(
            String id,
            String merchantId,
            String name,
            String category,
            BigDecimal price,
            BigDecimal originalPrice,
            Integer stock,
            Integer sold,
            String status,
            String image,
            String description,
            List<String> tags,
            LocalDate createdAt
    ) {
    }

    public record PublicMerchantView(
            String id,
            String name,
            String location,
            String description,
            String banner,
            String accent,
            String status,
            BigDecimal rating,
            LocalDate joinedAt
    ) {
    }

    public record StorefrontView(
            List<PublicMerchantView> merchants,
            List<ProductView> products,
            List<ReviewView> reviews
    ) {
    }

    public record ReviewView(
            Long id,
            String userId,
            String reviewerName,
            String reviewerAvatar,
            String merchantId,
            String productId,
            String reviewType,
            Integer rating,
            String content,
            Boolean verifiedPurchase,
            String createdAt
    ) {
    }

    public record ReviewRequest(
            @NotNull(message = "请选择评分")
            @Min(value = 1, message = "评分最低为 1 星")
            @Max(value = 5, message = "评分最高为 5 星")
            Integer rating,
            @NotBlank(message = "请输入评价内容")
            @Size(max = 500, message = "评价内容不能超过 500 个字符")
            String content
    ) {
    }

    public record SupportConversationView(
            Long id,
            String merchantId,
            String merchantName,
            String merchantBanner,
            String userId,
            String buyerName,
            String buyerAvatar,
            String lastMessage,
            String lastMessageAt
    ) {
    }

    public record SupportMessageView(
            Long id,
            String senderId,
            String senderRole,
            String senderName,
            String content,
            String createdAt
    ) {
    }

    public record SupportConversationDetail(
            SupportConversationView conversation,
            List<SupportMessageView> messages
    ) {
    }

    public record SupportMessageRequest(
            @NotBlank(message = "请输入消息内容")
            @Size(max = 1000, message = "消息不能超过 1000 个字符")
            String content
    ) {
    }

    public record AfterSaleView(
            Long id,
            String orderId,
            String userId,
            String buyerName,
            String merchantId,
            String merchantName,
            String requestType,
            String reason,
            String description,
            String status,
            String merchantReply,
            BigDecimal orderTotal,
            String createdAt,
            String processedAt
    ) {
    }

    public record AfterSaleCreateRequest(
            @NotBlank(message = "请选择售后类型")
            String requestType,
            @NotBlank(message = "请输入售后原因")
            @Size(max = 300, message = "售后原因不能超过 300 个字符")
            String reason,
            @Size(max = 500, message = "问题描述不能超过 500 个字符")
            String description
    ) {
    }

    public record AfterSaleProcessRequest(
            @NotBlank(message = "请选择处理结果")
            String decision,
            @NotBlank(message = "请输入处理说明")
            @Size(max = 500, message = "处理说明不能超过 500 个字符")
            String reply
    ) {
    }

    public record AddressView(
            Long id,
            String receiverName,
            String receiverPhone,
            String region,
            String detail,
            String fullAddress,
            Boolean defaultAddress
    ) {
    }

    public record AddressRequest(
            @NotBlank(message = "请输入收货人")
            @Size(max = 50, message = "收货人不能超过 50 个字符")
            String receiverName,
            @NotBlank(message = "请输入联系电话")
            @Size(max = 30, message = "联系电话不能超过 30 个字符")
            String receiverPhone,
            @NotBlank(message = "请输入所在地区")
            @Size(max = 120, message = "所在地区不能超过 120 个字符")
            String region,
            @NotBlank(message = "请输入详细地址")
            @Size(max = 240, message = "详细地址不能超过 240 个字符")
            String detail,
            Boolean defaultAddress
    ) {
    }

    public record UserView(
            String id,
            String name,
            String email,
            String phone,
            String status,
            String level,
            LocalDate joinedAt,
            String city,
            String avatar
    ) {
    }

    public record CartLine(
            String productId,
            Integer quantity
    ) {
    }

    public record OrderItemView(
            String productId,
            Integer quantity,
            BigDecimal price
    ) {
    }

    public record OrderView(
            String id,
            String userId,
            String merchantId,
            List<OrderItemView> items,
            BigDecimal total,
            String status,
            String paymentNo,
            String paymentExpiresAt,
            String shippingCompany,
            String trackingNo,
            String shippedAt,
            String receiverName,
            String receiverPhone,
            String receiverAddress,
            String createdAt
    ) {
    }

    public record PaymentView(
            String paymentNo,
            List<String> orderIds,
            BigDecimal amount,
            String status,
            String expiresAt,
            String paidAt
    ) {
    }

    public record ShipOrderRequest(
            @NotBlank(message = "请选择物流公司")
            @Size(max = 80, message = "物流公司不能超过 80 个字符")
            String shippingCompany
    ) {
    }

    public record MerchantReviewRequest(
            @NotBlank(message = "请选择审核结果")
            String decision,
            @Size(max = 500, message = "审核说明不能超过 500 个字符")
            String note
    ) {
    }

    public record OperationLogView(
            Long id,
            String operatorName,
            String action,
            String targetType,
            String targetId,
            String targetName,
            String detail,
            String createdAt
    ) {
    }

    public record CheckoutRequest(
            @NotBlank(message = "请输入收货人") String receiverName,
            @NotBlank(message = "请输入联系电话") String receiverPhone,
            @NotBlank(message = "请输入收货地址") String receiverAddress
    ) {
    }

    public record BootstrapView(
            String role,
            AccountView account,
            List<MerchantView> merchants,
            List<ProductView> products,
            List<UserView> users,
            List<CartLine> cart,
            List<AddressView> addresses,
            List<ReviewView> reviews,
            List<AfterSaleView> afterSales,
            List<OperationLogView> operationLogs,
            List<OrderView> orders
    ) {
    }

    public record CartAddRequest(
            @NotBlank(message = "商品不能为空")
            String productId,
            @Min(value = 1, message = "购买数量至少为 1")
            int quantity
    ) {
    }

    public record CartUpdateRequest(
            @Min(value = 1, message = "购买数量至少为 1")
            int quantity
    ) {
    }

    public record PriceRequest(
            @NotNull(message = "请输入商品价格")
            @DecimalMin(value = "0.01", message = "价格必须大于 0")
            BigDecimal price
    ) {
    }

    public record StatusRequest(
            @NotBlank(message = "状态不能为空")
            String status
    ) {
    }

    public record CreateProductRequest(
            @NotBlank(message = "请输入商品名称")
            String name,
            @NotBlank(message = "请选择商品分类")
            String category,
            @NotNull(message = "请输入销售价")
            @DecimalMin(value = "0.01", message = "销售价必须大于 0")
            BigDecimal price,
            BigDecimal originalPrice,
            @NotNull(message = "请输入库存")
            @Min(value = 0, message = "库存不能小于 0")
            Integer stock,
            String status,
            String image,
            String description,
            String tags
    ) {
    }

    public record MerchantProfileRequest(
            @NotBlank(message = "请输入店铺名称")
            String name,
            @NotBlank(message = "请输入负责人")
            String owner,
            String phone,
            String location,
            String description
    ) {
    }

    public record ProductUpdateRequest(
            @NotNull(message = "请输入库存")
            @Min(value = 0, message = "库存不能小于 0")
            Integer stock,
            @NotBlank(message = "请选择分类")
            String category,
            String image,
            String description
    ) {
    }

    public record UserProfileRequest(
            @NotBlank(message = "请输入姓名")
            String name,
            String phone,
            String city
    ) {
    }
}
