package com.yunshang.market.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {
    @TableId(type = IdType.INPUT)
    private String id;
    private String email;
    @JsonIgnore
    private String passwordHash;
    private String displayName;
    private String phone;
    private String city;
    private String role;
    private String memberLevel;
    private String avatar;
    private String merchantId;
    private String status;
    private Integer tokenVersion;
    private LocalDate joinedAt;
    private LocalDateTime createdAt;
}
