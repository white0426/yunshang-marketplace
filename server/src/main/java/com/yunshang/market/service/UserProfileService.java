package com.yunshang.market.service;

import com.yunshang.market.common.ApiException;
import com.yunshang.market.dto.ApiDtos.UserProfileRequest;
import com.yunshang.market.entity.SysUser;
import com.yunshang.market.mapper.SysUserMapper;
import com.yunshang.market.security.AuthPrincipal;
import org.springframework.stereotype.Service;

@Service
public class UserProfileService {
    private final SysUserMapper userMapper;

    public UserProfileService(SysUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public void update(AuthPrincipal principal, UserProfileRequest request) {
        SysUser user = userMapper.selectById(principal.userId());
        if (user == null) {
            throw ApiException.notFound("用户不存在");
        }
        user.setDisplayName(request.name().trim());
        user.setPhone(request.phone());
        user.setCity(request.city());
        user.setAvatar(request.name().trim().substring(0, 1));
        userMapper.updateById(user);
    }
}
