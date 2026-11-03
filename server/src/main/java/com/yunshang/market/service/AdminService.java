package com.yunshang.market.service;

import com.yunshang.market.common.ApiException;
import com.yunshang.market.dto.ApiDtos.MerchantReviewRequest;
import com.yunshang.market.entity.Merchant;
import com.yunshang.market.entity.Product;
import com.yunshang.market.entity.SysUser;
import com.yunshang.market.mapper.MerchantMapper;
import com.yunshang.market.mapper.ProductMapper;
import com.yunshang.market.mapper.SysUserMapper;
import com.yunshang.market.security.AuthPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@Transactional
public class AdminService {
    private final MerchantMapper merchantMapper;
    private final ProductMapper productMapper;
    private final SysUserMapper userMapper;
    private final AuditLogService auditLogService;

    public AdminService(
            MerchantMapper merchantMapper,
            ProductMapper productMapper,
            SysUserMapper userMapper,
            AuditLogService auditLogService
    ) {
        this.merchantMapper = merchantMapper;
        this.productMapper = productMapper;
        this.userMapper = userMapper;
        this.auditLogService = auditLogService;
    }

    public void updateMerchantStatus(
            AuthPrincipal principal,
            String merchantId,
            String status
    ) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw ApiException.notFound("商家不存在");
        }
        String previous = merchant.getStatus();
        String next = normalizeAccountStatus(status);
        merchant.setStatus(next);
        merchantMapper.updateById(merchant);
        auditLogService.record(
                principal,
                "ACTIVE".equals(next) ? "启用商家" : "停用商家",
                "MERCHANT",
                merchant.getId(),
                merchant.getName(),
                previous + " -> " + next
        );
    }

    public void reviewMerchant(
            AuthPrincipal principal,
            String merchantId,
            MerchantReviewRequest request
    ) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw ApiException.notFound("商家不存在");
        }
        if (!"PENDING".equals(merchant.getStatus())) {
            throw ApiException.badRequest("该商家入驻申请已经处理");
        }
        boolean approved;
        if ("APPROVE".equalsIgnoreCase(request.decision())
                || "通过".equals(request.decision())) {
            approved = true;
        } else if ("REJECT".equalsIgnoreCase(request.decision())
                || "驳回".equals(request.decision())) {
            approved = false;
        } else {
            throw ApiException.badRequest("审核结果不正确");
        }
        if (!approved && (request.note() == null || request.note().isBlank())) {
            throw ApiException.badRequest("驳回商家时必须填写审核说明");
        }
        merchant.setStatus(approved ? "ACTIVE" : "REJECTED");
        merchant.setReviewNote(
                request.note() == null ? null : request.note().trim()
        );
        merchant.setReviewedAt(LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
        merchantMapper.updateById(merchant);
        auditLogService.record(
                principal,
                approved ? "通过商家入驻" : "驳回商家入驻",
                "MERCHANT",
                merchant.getId(),
                merchant.getName(),
                merchant.getReviewNote()
        );
    }

    public void updateProductPrice(
            AuthPrincipal principal,
            String productId,
            BigDecimal price
    ) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw ApiException.notFound("商品不存在");
        }
        BigDecimal previous = product.getPrice();
        product.setPrice(price);
        productMapper.updateById(product);
        auditLogService.record(
                principal,
                "修改商品价格",
                "PRODUCT",
                product.getId(),
                product.getName(),
                previous + " -> " + price
        );
    }

    public void updateProductStatus(
            AuthPrincipal principal,
            String productId,
            String status
    ) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw ApiException.notFound("商品不存在");
        }
        String previous = product.getStatus();
        if ("online".equalsIgnoreCase(status) || "ONLINE".equalsIgnoreCase(status)) {
            product.setStatus("ONLINE");
        } else if ("offline".equalsIgnoreCase(status) || "OFFLINE".equalsIgnoreCase(status)) {
            product.setStatus("OFFLINE");
        } else {
            throw ApiException.badRequest("商品状态不正确");
        }
        productMapper.updateById(product);
        auditLogService.record(
                principal,
                "ONLINE".equals(product.getStatus()) ? "恢复商品上架" : "强制商品下架",
                "PRODUCT",
                product.getId(),
                product.getName(),
                previous + " -> " + product.getStatus()
        );
    }

    public void updateUserStatus(
            AuthPrincipal principal,
            String userId,
            String status
    ) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw ApiException.notFound("用户不存在");
        }
        if ("ADMIN".equals(user.getRole())) {
            throw ApiException.badRequest("不能停用超级管理员账号");
        }
        String previous = user.getStatus();
        String next = normalizeAccountStatus(status);
        user.setStatus(next);
        userMapper.updateById(user);
        auditLogService.record(
                principal,
                "ACTIVE".equals(next) ? "恢复用户账号" : "停用用户账号",
                "USER",
                user.getId(),
                user.getDisplayName(),
                previous + " -> " + next
        );
    }

    private String normalizeAccountStatus(String status) {
        if ("active".equalsIgnoreCase(status) || "ACTIVE".equalsIgnoreCase(status)) {
            return "ACTIVE";
        }
        if ("disabled".equalsIgnoreCase(status) || "DISABLED".equalsIgnoreCase(status)) {
            return "DISABLED";
        }
        throw ApiException.badRequest("账号状态不正确");
    }
}
