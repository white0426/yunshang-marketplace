package com.yunshang.market.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yunshang.market.common.ApiException;
import com.yunshang.market.dto.ApiDtos.LoginRequest;
import com.yunshang.market.dto.ApiDtos.LoginResponse;
import com.yunshang.market.dto.ApiDtos.MerchantRegisterRequest;
import com.yunshang.market.dto.ApiDtos.MerchantRegisterResponse;
import com.yunshang.market.dto.ApiDtos.RegisterRequest;
import com.yunshang.market.entity.Merchant;
import com.yunshang.market.entity.SysUser;
import com.yunshang.market.mapper.MerchantMapper;
import com.yunshang.market.mapper.SysUserMapper;
import com.yunshang.market.security.AuthPrincipal;
import com.yunshang.market.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Service
public class AuthService {
    private final SysUserMapper userMapper;
    private final MerchantMapper merchantMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final BootstrapService bootstrapService;

    public AuthService(
            SysUserMapper userMapper,
            MerchantMapper merchantMapper,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            BootstrapService bootstrapService
    ) {
        this.userMapper = userMapper;
        this.merchantMapper = merchantMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.bootstrapService = bootstrapService;
    }

    public LoginResponse login(LoginRequest request) {
        SysUser user = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getEmail, request.email().trim().toLowerCase())
                .last("LIMIT 1"));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw ApiException.unauthorized("账号或密码不正确");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw ApiException.forbidden("该账号已停用，请联系平台管理员");
        }
        if ("MERCHANT".equals(user.getRole())) {
            Merchant merchant = merchantMapper.selectById(user.getMerchantId());
            if (merchant == null) {
                throw ApiException.forbidden("商家信息不存在，请联系平台管理员");
            }
            if ("PENDING".equals(merchant.getStatus())) {
                throw ApiException.forbidden("商家入驻申请正在审核中，请耐心等待");
            }
            if ("REJECTED".equals(merchant.getStatus())) {
                throw ApiException.forbidden("商家入驻申请未通过，请联系平台管理员");
            }
            if (!"ACTIVE".equals(merchant.getStatus())) {
                throw ApiException.forbidden("该商家账号已停用，请联系平台管理员");
            }
        }

        String path = switch (user.getRole()) {
            case "MERCHANT" -> "/merchant";
            case "ADMIN" -> "/admin";
            default -> "/";
        };
        return new LoginResponse(
                jwtService.generateToken(user),
                user.getRole().toLowerCase(),
                path,
                bootstrapService.accountView(user)
        );
    }

    public LoginResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        Long existing = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getEmail, email));
        if (existing > 0) {
            throw ApiException.badRequest("该邮箱已注册，请直接登录");
        }

        String displayName = request.displayName().trim();
        SysUser user = new SysUser();
        user.setId("u" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(displayName);
        user.setPhone(request.phone().trim());
        user.setCity("待完善");
        user.setRole("USER");
        user.setMemberLevel("普通会员");
        user.setAvatar(displayName.substring(0, 1));
        user.setStatus("ACTIVE");
        user.setTokenVersion(0);
        user.setJoinedAt(LocalDate.now(ZoneId.of("Asia/Shanghai")));
        try {
            userMapper.insert(user);
            userMapper.addRole(user.getId(), "r_user");
        } catch (DuplicateKeyException exception) {
            throw ApiException.badRequest("该邮箱已注册，请直接登录");
        }
        return login(new LoginRequest(email, request.password()));
    }

    @Transactional
    public MerchantRegisterResponse registerMerchant(MerchantRegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        String displayName = request.ownerName().trim();
        Long existingUser = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getEmail, email));
        Long existingMerchant = merchantMapper.selectCount(Wrappers.<Merchant>lambdaQuery()
                .eq(Merchant::getEmail, email));
        if (existingUser > 0 || existingMerchant > 0) {
            throw ApiException.badRequest("该邮箱已注册，请直接登录");
        }

        LocalDate joinedAt = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        Merchant merchant = new Merchant();
        merchant.setId("m" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        merchant.setName(request.shopName().trim());
        merchant.setOwnerName(displayName);
        merchant.setEmail(email);
        merchant.setPhone(request.phone().trim());
        merchant.setLocation(request.city().trim());
        merchant.setDescription(
                request.description() == null || request.description().isBlank()
                        ? "新入驻商家，正在完善店铺介绍。"
                        : request.description().trim()
        );
        merchant.setBanner("/images/chair.jpg");
        merchant.setAccent("#2d6a4f");
        merchant.setStatus("PENDING");
        merchant.setRating(new BigDecimal("5.0"));
        merchant.setJoinedAt(joinedAt);

        SysUser user = new SysUser();
        user.setId("mu" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(displayName);
        user.setPhone(request.phone().trim());
        user.setCity(request.city().trim());
        user.setRole("MERCHANT");
        user.setAvatar(displayName.substring(0, 1));
        user.setMerchantId(merchant.getId());
        user.setStatus("ACTIVE");
        user.setTokenVersion(0);
        user.setJoinedAt(joinedAt);

        try {
            merchantMapper.insert(merchant);
            userMapper.insert(user);
            userMapper.addRole(user.getId(), "r_merchant");
        } catch (DuplicateKeyException exception) {
            throw ApiException.badRequest("该邮箱已注册，请直接登录");
        }
        return new MerchantRegisterResponse(
                true,
                "商家入驻申请已提交，审核通过后即可登录商家后台"
        );
    }

    public SysUser verifyCurrent(AuthPrincipal principal) {
        SysUser user = userMapper.selectById(principal.userId());
        if (user == null || !"ACTIVE".equals(user.getStatus())) {
            throw ApiException.unauthorized("登录状态已失效，请重新登录");
        }
        return user;
    }

    public void logout(AuthPrincipal principal) {
        SysUser user = userMapper.selectById(principal.userId());
        if (user != null) {
            user.setTokenVersion((user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1);
            userMapper.updateById(user);
        }
    }
}
