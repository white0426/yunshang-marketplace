package com.yunshang.market.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunshang.market.entity.MerchantDailyStat;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface MerchantDailyStatMapper extends BaseMapper<MerchantDailyStat> {
    @Insert("""
            INSERT INTO merchant_daily_stat (merchant_id, stat_date, sales_amount, order_count)
            VALUES (#{merchantId}, #{statDate}, #{amount}, 1)
            ON DUPLICATE KEY UPDATE
                sales_amount = sales_amount + VALUES(sales_amount),
                order_count = order_count + 1
            """)
    int addOrderStat(String merchantId, LocalDate statDate, BigDecimal amount);

    @Update("""
            UPDATE merchant_daily_stat
            SET sales_amount = GREATEST(sales_amount - #{amount}, 0),
                order_count = GREATEST(order_count - 1, 0)
            WHERE merchant_id = #{merchantId}
              AND stat_date = #{statDate}
            """)
    int subtractOrderStat(String merchantId, LocalDate statDate, BigDecimal amount);
}
