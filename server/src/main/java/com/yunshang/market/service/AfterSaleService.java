package com.yunshang.market.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yunshang.market.common.ApiException;
import com.yunshang.market.dto.ApiDtos.AfterSaleCreateRequest;
import com.yunshang.market.dto.ApiDtos.AfterSaleProcessRequest;
import com.yunshang.market.dto.ApiDtos.AfterSaleView;
import com.yunshang.market.entity.AfterSaleRequest;
import com.yunshang.market.entity.Merchant;
import com.yunshang.market.entity.OrderItem;
import com.yunshang.market.entity.ShopOrder;
import com.yunshang.market.entity.SysUser;
import com.yunshang.market.mapper.AfterSaleRequestMapper;
import com.yunshang.market.mapper.MerchantDailyStatMapper;
import com.yunshang.market.mapper.MerchantMapper;
import com.yunshang.market.mapper.OrderItemMapper;
import com.yunshang.market.mapper.ProductMapper;
import com.yunshang.market.mapper.ShopOrderMapper;
import com.yunshang.market.mapper.SysUserMapper;
import com.yunshang.market.security.AuthPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AfterSaleService {
    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AfterSaleRequestMapper afterSaleMapper;
    private final ShopOrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final MerchantDailyStatMapper statMapper;
    private final MerchantMapper merchantMapper;
    private final SysUserMapper userMapper;

    public AfterSaleService(
            AfterSaleRequestMapper afterSaleMapper,
            ShopOrderMapper orderMapper,
            OrderItemMapper orderItemMapper,
            ProductMapper productMapper,
            MerchantDailyStatMapper statMapper,
            MerchantMapper merchantMapper,
            SysUserMapper userMapper
    ) {
        this.afterSaleMapper = afterSaleMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.productMapper = productMapper;
        this.statMapper = statMapper;
        this.merchantMapper = merchantMapper;
        this.userMapper = userMapper;
    }

    public List<AfterSaleView> list(AuthPrincipal principal) {
        List<AfterSaleRequest> requests;
        if ("ADMIN".equals(principal.role())) {
            requests = afterSaleMapper.selectList(Wrappers.<AfterSaleRequest>lambdaQuery()
                    .orderByDesc(AfterSaleRequest::getCreatedAt));
        } else if ("MERCHANT".equals(principal.role())) {
            requests = afterSaleMapper.selectList(Wrappers.<AfterSaleRequest>lambdaQuery()
                    .eq(AfterSaleRequest::getMerchantId, requireMerchantId(principal))
                    .orderByDesc(AfterSaleRequest::getCreatedAt));
        } else {
            requests = afterSaleMapper.selectList(Wrappers.<AfterSaleRequest>lambdaQuery()
                    .eq(AfterSaleRequest::getUserId, principal.userId())
                    .orderByDesc(AfterSaleRequest::getCreatedAt));
        }
        if (requests.isEmpty()) {
            return List.of();
        }

        List<String> orderIds = requests.stream()
                .map(AfterSaleRequest::getOrderId)
                .toList();
        Map<String, ShopOrder> orders = orderMapper.selectBatchIds(orderIds).stream()
                .collect(Collectors.toMap(ShopOrder::getId, Function.identity()));
        Map<String, SysUser> users = userMapper.selectBatchIds(
                        requests.stream().map(AfterSaleRequest::getUserId).distinct().toList()
                )
                .stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        Map<String, Merchant> merchants = merchantMapper.selectBatchIds(
                        requests.stream().map(AfterSaleRequest::getMerchantId).distinct().toList()
                )
                .stream()
                .collect(Collectors.toMap(Merchant::getId, Function.identity()));

        return requests.stream()
                .map(request -> {
                    ShopOrder order = orders.get(request.getOrderId());
                    SysUser buyer = users.get(request.getUserId());
                    Merchant merchant = merchants.get(request.getMerchantId());
                    return new AfterSaleView(
                            request.getId(),
                            request.getOrderId(),
                            request.getUserId(),
                            buyer == null ? "用户" : buyer.getDisplayName(),
                            request.getMerchantId(),
                            merchant == null ? "商家" : merchant.getName(),
                            request.getRequestType(),
                            request.getReason(),
                            request.getDescription(),
                            request.getStatus(),
                            request.getMerchantReply(),
                            order == null ? null : order.getTotal(),
                            request.getCreatedAt().format(DATE_TIME_FORMAT),
                            request.getProcessedAt() == null
                                    ? null
                                    : request.getProcessedAt().format(DATE_TIME_FORMAT)
                    );
                })
                .toList();
    }

    @Transactional
    public void create(AuthPrincipal principal, String orderId, AfterSaleCreateRequest request) {
        ShopOrder order = requireUserOrder(principal, orderId);
        if (!List.of("待发货", "待收货", "已完成").contains(order.getStatus())) {
            throw ApiException.badRequest("当前订单状态不能申请售后");
        }
        Long existing = afterSaleMapper.selectCount(Wrappers.<AfterSaleRequest>lambdaQuery()
                .eq(AfterSaleRequest::getOrderId, orderId));
        if (existing > 0) {
            throw ApiException.badRequest("该订单已经提交过售后申请");
        }
        if (!List.of("仅退款", "退货退款").contains(request.requestType())) {
            throw ApiException.badRequest("售后类型不正确");
        }

        AfterSaleRequest afterSale = new AfterSaleRequest();
        afterSale.setOrderId(orderId);
        afterSale.setUserId(principal.userId());
        afterSale.setMerchantId(order.getMerchantId());
        afterSale.setRequestType(request.requestType());
        afterSale.setReason(request.reason().trim());
        afterSale.setDescription(
                request.description() == null ? null : request.description().trim()
        );
        afterSale.setStatus("待商家处理");
        afterSaleMapper.insert(afterSale);
    }

    @Transactional
    public void process(
            AuthPrincipal principal,
            Long afterSaleId,
            AfterSaleProcessRequest request
    ) {
        AfterSaleRequest afterSale = afterSaleMapper.selectById(afterSaleId);
        if (afterSale == null) {
            throw ApiException.notFound("售后申请不存在");
        }
        if (!requireMerchantId(principal).equals(afterSale.getMerchantId())) {
            throw ApiException.forbidden("不能处理其他商家的售后申请");
        }
        if (!"待商家处理".equals(afterSale.getStatus())) {
            throw ApiException.badRequest("该售后申请已经处理");
        }

        boolean approved;
        if ("APPROVE".equalsIgnoreCase(request.decision())
                || "同意".equals(request.decision())) {
            approved = true;
        } else if ("REJECT".equalsIgnoreCase(request.decision())
                || "拒绝".equals(request.decision())) {
            approved = false;
        } else {
            throw ApiException.badRequest("处理结果不正确");
        }

        afterSale.setStatus(approved ? "已同意" : "已拒绝");
        afterSale.setMerchantReply(request.reply().trim());
        afterSale.setProcessedAt(LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
        afterSaleMapper.updateById(afterSale);

        ShopOrder order = orderMapper.selectById(afterSale.getOrderId());
        if (approved && order != null && "待发货".equals(order.getStatus())) {
            rollbackPendingOrder(order);
        }
    }

    private void rollbackPendingOrder(ShopOrder order) {
        List<OrderItem> items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderId, order.getId()));
        for (OrderItem item : items) {
            productMapper.restoreStock(item.getProductId(), item.getQuantity());
        }
        statMapper.subtractOrderStat(
                order.getMerchantId(),
                order.getCreatedAt().toLocalDate(),
                order.getTotal()
        );
        order.setStatus("已取消");
        orderMapper.updateById(order);
    }

    private ShopOrder requireUserOrder(AuthPrincipal principal, String orderId) {
        ShopOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw ApiException.notFound("订单不存在");
        }
        if (!principal.userId().equals(order.getUserId())) {
            throw ApiException.forbidden("不能操作其他用户的订单");
        }
        return order;
    }

    private String requireMerchantId(AuthPrincipal principal) {
        if (principal.merchantId() == null || principal.merchantId().isBlank()) {
            throw ApiException.forbidden("当前账号未绑定商家");
        }
        return principal.merchantId();
    }
}
