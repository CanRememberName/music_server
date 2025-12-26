package com.music.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.music.server.model.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    // private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${app.auth.token-ttl-days}")
    private int tokenTtlDays;

    public AuthInterceptor(/*StringRedisTemplate redisTemplate,*/ ObjectMapper objectMapper) {
        // this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        log.info("preHandle, start");
//        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
//            return true;
//        }
//
//        String authHeader = request.getHeader("Authorization");
//        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
//            writeError(response);
//            return false;
//        }

//        String token = authHeader.substring(7);
//        String key = "auth:token:" + token;
//
//        String username = redisTemplate.opsForValue().get(key);
//        if (username == null) {
//            writeError(response);
//            return false;
//        }
//
//        // Slide expiration
//        redisTemplate.expire(key, tokenTtlDays, TimeUnit.DAYS);
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
