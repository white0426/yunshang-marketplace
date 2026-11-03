package com.yunshang.market.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yunshang.market.common.ApiException;
import com.yunshang.market.dto.ApiDtos.SupportConversationDetail;
import com.yunshang.market.dto.ApiDtos.SupportConversationView;
import com.yunshang.market.dto.ApiDtos.SupportMessageRequest;
import com.yunshang.market.dto.ApiDtos.SupportMessageView;
import com.yunshang.market.entity.Merchant;
import com.yunshang.market.entity.SupportConversation;
import com.yunshang.market.entity.SupportMessage;
import com.yunshang.market.entity.SysUser;
import com.yunshang.market.mapper.MerchantMapper;
import com.yunshang.market.mapper.SupportConversationMapper;
import com.yunshang.market.mapper.SupportMessageMapper;
import com.yunshang.market.mapper.SysUserMapper;
import com.yunshang.market.security.AuthPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Service
public class SupportService {
    private static final DateTimeFormatter MESSAGE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter CONVERSATION_TIME =
            DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final SupportConversationMapper conversationMapper;
    private final SupportMessageMapper messageMapper;
    private final MerchantMapper merchantMapper;
    private final SysUserMapper userMapper;

    public SupportService(
            SupportConversationMapper conversationMapper,
            SupportMessageMapper messageMapper,
            MerchantMapper merchantMapper,
            SysUserMapper userMapper
    ) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.merchantMapper = merchantMapper;
        this.userMapper = userMapper;
    }

    public List<SupportConversationView> userConversations(AuthPrincipal principal) {
        List<Merchant> merchants = merchantMapper.selectList(Wrappers.<Merchant>lambdaQuery()
                .eq(Merchant::getStatus, "ACTIVE")
                .orderByAsc(Merchant::getJoinedAt));
        return merchants.stream()
                .map(merchant -> {
                    SupportConversation conversation = findConversation(
                            principal.userId(),
                            merchant.getId()
                    );
                    return toConversationView(conversation, merchant, null);
                })
                .toList();
    }

    public SupportConversationDetail userMessages(
            AuthPrincipal principal,
            String merchantId
    ) {
        Merchant merchant = requireActiveMerchant(merchantId);
        SupportConversation conversation = findConversation(principal.userId(), merchantId);
        return detail(conversation, merchant, null);
    }

    @Transactional
    public SupportConversationDetail sendUserMessage(
            AuthPrincipal principal,
            String merchantId,
            SupportMessageRequest request
    ) {
        Merchant merchant = requireActiveMerchant(merchantId);
        SupportConversation conversation = findConversation(principal.userId(), merchantId);
        if (conversation == null) {
            conversation = createConversation(principal.userId(), merchantId);
        }
        addMessage(conversation, principal.userId(), "USER", request.content());
        return detail(conversation, merchant, null);
    }

    public List<SupportConversationView> merchantConversations(AuthPrincipal principal) {
        String merchantId = requireMerchantId(principal);
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw ApiException.notFound("商家不存在");
        }
        return conversationMapper.selectList(Wrappers.<SupportConversation>lambdaQuery()
                        .eq(SupportConversation::getMerchantId, merchantId)
                        .isNotNull(SupportConversation::getLastMessageAt)
                        .orderByDesc(SupportConversation::getLastMessageAt))
                .stream()
                .map(conversation -> {
                    SysUser buyer = userMapper.selectById(conversation.getUserId());
                    return toConversationView(conversation, merchant, buyer);
                })
                .sorted(Comparator.comparing(
                        SupportConversationView::lastMessageAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList();
    }

    public SupportConversationDetail merchantMessages(
            AuthPrincipal principal,
            String userId
    ) {
        String merchantId = requireMerchantId(principal);
        Merchant merchant = merchantMapper.selectById(merchantId);
        SupportConversation conversation = findConversation(userId, merchantId);
        return detail(conversation, merchant, userMapper.selectById(userId));
    }

    @Transactional
    public SupportConversationDetail sendMerchantMessage(
            AuthPrincipal principal,
            String userId,
            SupportMessageRequest request
    ) {
        String merchantId = requireMerchantId(principal);
        Merchant merchant = merchantMapper.selectById(merchantId);
        SysUser buyer = userMapper.selectById(userId);
        if (buyer == null || !"USER".equals(buyer.getRole())) {
            throw ApiException.notFound("用户不存在");
        }
        SupportConversation conversation = findConversation(userId, merchantId);
        if (conversation == null) {
            conversation = createConversation(userId, merchantId);
        }
        addMessage(conversation, principal.userId(), "MERCHANT", request.content());
        return detail(conversation, merchant, buyer);
    }

    private SupportConversation findConversation(String userId, String merchantId) {
        return conversationMapper.selectOne(Wrappers.<SupportConversation>lambdaQuery()
                .eq(SupportConversation::getUserId, userId)
                .eq(SupportConversation::getMerchantId, merchantId)
                .last("LIMIT 1"));
    }

    private SupportConversation createConversation(String userId, String merchantId) {
        SupportConversation conversation = new SupportConversation();
        conversation.setUserId(userId);
        conversation.setMerchantId(merchantId);
        conversationMapper.insert(conversation);
        return conversation;
    }

    private void addMessage(
            SupportConversation conversation,
            String senderId,
            String senderRole,
            String content
    ) {
        SupportMessage message = new SupportMessage();
        message.setConversationId(conversation.getId());
        message.setSenderId(senderId);
        message.setSenderRole(senderRole);
        message.setContent(content.trim());
        messageMapper.insert(message);
        conversation.setLastMessageAt(LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
        conversationMapper.updateById(conversation);
    }

    private SupportConversationDetail detail(
            SupportConversation conversation,
            Merchant merchant,
            SysUser buyer
    ) {
        SupportConversationView conversationView =
                toConversationView(conversation, merchant, buyer);
        if (conversation == null) {
            return new SupportConversationDetail(conversationView, List.of());
        }
        List<SupportMessage> messages = messageMapper.selectList(
                Wrappers.<SupportMessage>lambdaQuery()
                        .eq(SupportMessage::getConversationId, conversation.getId())
                        .orderByAsc(SupportMessage::getCreatedAt)
                        .orderByAsc(SupportMessage::getId)
        );
        SysUser merchantUser = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getMerchantId, merchant.getId())
                .eq(SysUser::getRole, "MERCHANT")
                .last("LIMIT 1"));
        String buyerName = buyer == null ? "用户" : buyer.getDisplayName();
        String merchantName = merchantUser == null
                ? merchant.getOwnerName()
                : merchantUser.getDisplayName();
        List<SupportMessageView> views = messages.stream()
                .map(message -> new SupportMessageView(
                        message.getId(),
                        message.getSenderId(),
                        message.getSenderRole(),
                        "MERCHANT".equals(message.getSenderRole()) ? merchantName : buyerName,
                        message.getContent(),
                        message.getCreatedAt().format(MESSAGE_TIME)
                ))
                .toList();
        return new SupportConversationDetail(conversationView, views);
    }

    private SupportConversationView toConversationView(
            SupportConversation conversation,
            Merchant merchant,
            SysUser buyer
    ) {
        SupportMessage lastMessage = conversation == null
                ? null
                : messageMapper.selectOne(Wrappers.<SupportMessage>lambdaQuery()
                        .eq(SupportMessage::getConversationId, conversation.getId())
                        .orderByDesc(SupportMessage::getCreatedAt)
                        .orderByDesc(SupportMessage::getId)
                        .last("LIMIT 1"));
        SysUser resolvedBuyer = buyer;
        if (resolvedBuyer == null && conversation != null) {
            resolvedBuyer = userMapper.selectById(conversation.getUserId());
        }
        return new SupportConversationView(
                conversation == null ? null : conversation.getId(),
                merchant.getId(),
                merchant.getName(),
                merchant.getBanner(),
                conversation == null ? null : conversation.getUserId(),
                resolvedBuyer == null ? null : resolvedBuyer.getDisplayName(),
                resolvedBuyer == null ? "用" : resolvedBuyer.getAvatar(),
                lastMessage == null ? null : lastMessage.getContent(),
                conversation == null || conversation.getLastMessageAt() == null
                        ? null
                        : conversation.getLastMessageAt().format(CONVERSATION_TIME)
        );
    }

    private Merchant requireActiveMerchant(String merchantId) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null || !"ACTIVE".equals(merchant.getStatus())) {
            throw ApiException.notFound("店铺不存在或已暂停营业");
        }
        return merchant;
    }

    private String requireMerchantId(AuthPrincipal principal) {
        if (principal.merchantId() == null || principal.merchantId().isBlank()) {
            throw ApiException.forbidden("当前账号未绑定商家");
        }
        return principal.merchantId();
    }
}
