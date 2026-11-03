package com.yunshang.market.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunshang.market.entity.Product;
import org.apache.ibatis.annotations.Update;

public interface ProductMapper extends BaseMapper<Product> {
    @Update("""
            UPDATE product
            SET stock = stock - #{quantity},
                sold = sold + #{quantity}
            WHERE id = #{productId}
              AND status = 'ONLINE'
              AND stock >= #{quantity}
            """)
    int deductStock(String productId, int quantity);

    @Update("""
            UPDATE product
            SET stock = stock + #{quantity},
                sold = GREATEST(sold - #{quantity}, 0)
            WHERE id = #{productId}
            """)
    int restoreStock(String productId, int quantity);
}
