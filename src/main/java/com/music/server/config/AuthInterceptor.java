package com.music.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.music.server.model.ApiResponse;
import com.music.server.model.User;
import com.music.server.utils.JsonUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.music.server.constants.RedisKeyConstant.*;

@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${app.auth.token-ttl-days}")
    private int tokenTtlDays;

    public AuthInterceptor(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
         this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            writeError(response);
            return false;
        }

        String token = authHeader.substring(7);
        String key = getTokenRedisKey(token);

        String userJson = redisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(userJson)) {
            writeError(response);
            return false;
        }
        User user = JsonUtil.toObject(userJson, User.class);
        if (Objects.isNull(user) || Objects.isNull(user.getId()) || Objects.isNull(user.getUsername()) || CollectionUtils.isEmpty(user.getRoles())) {
            writeError(response);
            return false;
        }
        // Slide expiration
        redisTemplate.expire(key, tokenTtlDays, TimeUnit.DAYS);
        redisTemplate.expire(getUserTokenRedisKey(user.getUsername()), tokenTtlDays, TimeUnit.DAYS);
        // 校验一下角色
        String requestURI = request.getRequestURI();
        if (ONLY_ADMIN_URIS.contains(requestURI)) {
            if (!user.getRoles().contains(ROLE_ADMIN)) {
                writeError(response);
                return false;
            }
        }
        return true;
    }

    private void writeError(HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_OK); // Return 200 as per common JSON practice, or 401 if strict. Frontend plan says code 401.
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        // Frontend plan: code 401 means Unauthorized
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(401, "Unauthorized")));
    }
}
