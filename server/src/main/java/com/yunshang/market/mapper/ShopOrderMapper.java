package com.yunshang.market.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunshang.market.entity.ShopOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ShopOrderMapper extends BaseMapper<ShopOrder> {
    @Select("""
            SELECT *
            FROM orders
            WHERE payment_no = #{paymentNo}
            ORDER BY created_at ASC, id ASC
            FOR UPDATE
            """)
    List<ShopOrder> selectByPaymentNoForUpdate(@Param("paymentNo") String paymentNo);
}
