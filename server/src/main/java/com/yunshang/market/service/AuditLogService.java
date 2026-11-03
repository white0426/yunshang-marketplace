package com.yunshang.market.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yunshang.market.dto.ApiDtos.OperationLogView;
import com.yunshang.market.entity.OperationLog;
import com.yunshang.market.entity.SysUser;
import com.yunshang.market.mapper.OperationLogMapper;
import com.yunshang.market.mapper.SysUserMapper;
import com.yunshang.market.security.AuthPrincipal;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AuditLogService {
    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final OperationLogMapper logMapper;
    private final SysUserMapper userMapper;

    public AuditLogService(
            OperationLogMapper logMapper,
            SysUserMapper userMapper
    ) {
        this.logMapper = logMapper;
        this.userMapper = userMapper;
    }

    public void record(
            AuthPrincipal principal,
            String action,
            String targetType,
            String targetId,
            String targetName,
            String detail
    ) {
        SysUser operator = userMapper.selectById(principal.userId());
        OperationLog log = new OperationLog();
        log.setOperatorId(principal.userId());
        log.setOperatorName(
                operator == null ? principal.email() : operator.getDisplayName()
        );
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setTargetName(targetName);
        log.setDetail(detail);
        logMapper.insert(log);
    }

    public List<OperationLogView> list() {
        return logMapper.selectList(Wrappers.<OperationLog>lambdaQuery()
                        .orderByDesc(OperationLog::getCreatedAt)
                        .orderByDesc(OperationLog::getId)
                        .last("LIMIT 100"))
                .stream()
                .map(log -> new OperationLogView(
                        log.getId(),
                        log.getOperatorName(),
                        log.getAction(),
                        log.getTargetType(),
                        log.getTargetId(),
                        log.getTargetName(),
                        log.getDetail(),
                        log.getCreatedAt().format(DATE_TIME_FORMAT)
                ))
                .toList();
    }
}
