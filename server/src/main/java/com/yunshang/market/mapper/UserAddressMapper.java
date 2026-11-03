package com.yunshang.market.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunshang.market.entity.UserAddress;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface UserAddressMapper extends BaseMapper<UserAddress> {
    @Select("""
            SELECT *
            FROM user_address
            WHERE user_id = #{userId}
            ORDER BY is_default DESC, updated_at DESC, id DESC
            """)
    List<UserAddress> selectByUser(@Param("userId") String userId);

    @Select("SELECT COUNT(*) FROM user_address WHERE user_id = #{userId}")
    long countByUser(@Param("userId") String userId);

    @Update("""
            UPDATE user_address
            SET is_default = 0
            WHERE user_id = #{userId} AND is_default = 1
            """)
    int clearDefault(@Param("userId") String userId);

    @Select("""
            SELECT *
            FROM user_address
            WHERE user_id = #{userId}
            ORDER BY updated_at DESC, id DESC
            LIMIT 1
            """)
    UserAddress selectMostRecent(@Param("userId") String userId);
}
