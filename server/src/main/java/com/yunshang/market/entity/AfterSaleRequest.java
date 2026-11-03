package com.yunshang.market.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("after_sale_request")
public class AfterSaleRequest {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderId;
    private String userId;
    private String merchantId;
    private String requestType;
    private String reason;
    private String description;
    private String status;
    private String merchantReply;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}
