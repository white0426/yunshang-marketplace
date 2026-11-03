package com.yunshang.market.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("orders")
public class ShopOrder {
    @TableId(type = IdType.INPUT)
    private String id;
    private String userId;
    private String merchantId;
    private BigDecimal total;
    private String status;
    private String paymentNo;
    private LocalDateTime paymentExpiresAt;
    private LocalDateTime paidAt;
    private String shippingCompany;
    private String trackingNo;
    private LocalDateTime shippedAt;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private LocalDateTime createdAt;
}
