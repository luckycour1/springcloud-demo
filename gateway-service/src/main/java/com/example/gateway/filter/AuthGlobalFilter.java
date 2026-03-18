package com.example.gateway.filter;

import com.example.common.util.JwtUtil;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "auth")
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    @Autowired
    private JwtUtil jwtUtil;

    @Setter
    private List<String> ignoreUrls = new ArrayList<>();
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 白名单提前放行
        for (String ignoreUrl : ignoreUrls) {
            if (pathMatcher.match(ignoreUrl, path)) {
                return chain.filter(exchange);
            }
        }

        // 获取 Authorization header 或 Cookie
        String header = request.getHeaders().getFirst("Authorization");
        String accessToken = null;
        if (header == null) {
            // 尝试从 Cookie 获取
            if (request.getCookies().containsKey("Authorization")) {
                var cookie = request.getCookies().getFirst("Authorization");
                if (cookie != null) {
                    accessToken = cookie.getValue();
                }
            }
        } else if (header.startsWith("Bearer ")) {
            accessToken = header.substring(7);
        }

        try {
            if (accessToken == null || !checkToken(accessToken)) {
                return onError(exchange, "登录已经过期,请重新登录");
            }

            // 传递用户信息到下游服务
            String username = jwtUtil.extractUsername(accessToken);
            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Name", username)
                    .build();
            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        } finally {
            removeOperator();
        }
    }

    /**
     * 清理上下文中的操作员信息，防止内存泄漏
     */
    private void removeOperator() {
        // 这里可根据你的实际上下文实现清理逻辑
        // 例如：OperatorContext.clear();
        // 如果无实际上下文可留空实现
    }

    /**
     * 校验token有效性（JWT 签名 + 有效期，无需缓存）
     */
    private boolean checkToken(String token) {
        try {
            String username = jwtUtil.extractUsername(token);
            return jwtUtil.validateToken(token, username);
        } catch (Exception e) {
            return false;
        }
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().set("Content-Type", "application/problem+json;charset=UTF-8");
        // 优雅的异常响应，使用对象转JSON而非字符串拼接
        var map = new java.util.LinkedHashMap<String, Object>();
        map.put("code", 401);
        map.put("msg", err);
        var data = new java.util.HashMap<String, Object>();
        data.put("authUrl", "/auth/login");
        map.put("data", data);
        map.put("timestamp", java.time.OffsetDateTime.now());
        String body;
        try {
            body = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(map);
        } catch (Exception e) {
            body = "{\"code\":401,\"msg\":\"" + err + "\"}";
        }
        byte[] bytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return response.writeWith(reactor.core.publisher.Mono.just(response.bufferFactory().wrap(bytes)));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}

