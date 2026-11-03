package com.yunshang.market.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("review")
public class Review {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String userId;
    private String merchantId;
    private String productId;
    private String orderId;
    private String reviewType;
    private Integer rating;
    private String content;
    private Boolean verifiedPurchase;
    private LocalDateTime createdAt;
}
