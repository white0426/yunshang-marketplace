package com.yunshang.market.security;

import com.yunshang.market.entity.Merchant;
import com.yunshang.market.entity.SysUser;
import com.yunshang.market.mapper.MerchantMapper;
import com.yunshang.market.mapper.SysUserMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final SysUserMapper userMapper;
    private final MerchantMapper merchantMapper;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            SysUserMapper userMapper,
            MerchantMapper merchantMapper
    ) {
        this.jwtService = jwtService;
        this.userMapper = userMapper;
        this.merchantMapper = merchantMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null
                && authorization.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                JwtService.TokenData token = jwtService.parse(authorization.substring(7));
                SysUser user = userMapper.selectById(token.userId());
                if (user == null
                        || !"ACTIVE".equals(user.getStatus())
                        || !token.email().equals(user.getEmail())
                        || token.tokenVersion() == null
                        || token.tokenVersion() != user.getTokenVersion()) {
                    filterChain.doFilter(request, response);
                    return;
                }
                if ("MERCHANT".equals(user.getRole())) {
                    Merchant merchant = merchantMapper.selectById(user.getMerchantId());
                    if (merchant == null || !"ACTIVE".equals(merchant.getStatus())) {
                        filterChain.doFilter(request, response);
                        return;
                    }
                }
                AuthPrincipal principal = new AuthPrincipal(
                        user.getId(),
                        user.getEmail(),
                        user.getRole(),
                        user.getMerchantId()
                );
                var authorities = new java.util.ArrayList<SimpleGrantedAuthority>();
                authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
                userMapper.selectPermissions(user.getId()).forEach(permission ->
                        authorities.add(new SimpleGrantedAuthority(permission)));
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                authorities
                        );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
