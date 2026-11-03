package com.yunshang.market.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("support_conversation")
public class SupportConversation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String userId;
    private String merchantId;
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;
}
