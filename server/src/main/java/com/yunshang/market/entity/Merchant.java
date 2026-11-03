package com.yunshang.market.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("merchant")
public class Merchant {
    @TableId(type = IdType.INPUT)
    private String id;
    private String name;
    private String ownerName;
    private String email;
    private String phone;
    private String location;
    private String description;
    private String banner;
    private String accent;
    private String status;
    private String reviewNote;
    private LocalDateTime reviewedAt;
    private BigDecimal rating;
    private LocalDate joinedAt;
    private LocalDateTime createdAt;
}
