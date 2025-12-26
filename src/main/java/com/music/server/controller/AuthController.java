package com.music.server.controller;

import com.music.server.model.ApiResponse;
import com.music.server.model.LoginRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {

    @Value("${app.auth.username}")
    private String adminUsername;

    @Value("${app.auth.password}")
    private String adminPassword;

    @Value("${app.auth.token-ttl-days}")
    private int tokenTtlDays;

    // private final StringRedisTemplate redisTemplate;

    public AuthController(/*StringRedisTemplate redisTemplate*/) {
        // this.redisTemplate = redisTemplate;
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody LoginRequest request) {
        log.info("login request: {}", request);
        if (!adminUsername.equals(request.getUsername()) || !adminPassword.equals(request.getPassword())) {
            return ApiResponse.error(1001, "Invalid username or password");
        }

        String token = UUID.randomUUID().toString();
        // Store simple user info in Redis (or just a flag)
        // Key: auth:token:{token} -> username
        String key = "auth:token:" + token;
        // redisTemplate.opsForValue().set(key, request.getUsername(), tokenTtlDays, TimeUnit.DAYS);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("expires_in", TimeUnit.DAYS.toSeconds(tokenTtlDays));
        
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", "1"); // Changed to String to prevent frontend type mismatch
        userInfo.put("nickname", "Admin");
        userInfo.put("avatar", "https://api.dicebear.com/7.x/avataaars/svg?seed=Admin");
        
        data.put("user_info", userInfo);
        ApiResponse<Map<String, Object>> result = ApiResponse.success(data);
        log.info("login result: {}", result);
        return result;
    }
}
