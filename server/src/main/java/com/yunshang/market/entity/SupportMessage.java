package com.yunshang.market.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("support_message")
public class SupportMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long conversationId;
    private String senderId;
    private String senderRole;
    private String content;
    private LocalDateTime createdAt;
}
