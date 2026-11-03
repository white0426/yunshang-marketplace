package com.yunshang.market.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("merchant_daily_stat")
public class MerchantDailyStat {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String merchantId;
    private LocalDate statDate;
    private BigDecimal salesAmount;
    private Integer orderCount;
}
