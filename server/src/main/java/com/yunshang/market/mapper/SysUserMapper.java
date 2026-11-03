package com.yunshang.market.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunshang.market.entity.SysUser;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface SysUserMapper extends BaseMapper<SysUser> {
    @Select("""
            SELECT DISTINCT m.permission_code
            FROM sys_menu m
            JOIN sys_role_menu rm ON rm.menu_id = m.id
            JOIN sys_user_role ur ON ur.role_id = rm.role_id
            WHERE ur.user_id = #{userId} AND m.status = 'ACTIVE'
            """)
    List<String> selectPermissions(String userId);

    @Insert("""
            INSERT IGNORE INTO sys_user_role (user_id, role_id)
            VALUES (#{userId}, #{roleId})
            """)
    int addRole(@Param("userId") String userId, @Param("roleId") String roleId);
}
