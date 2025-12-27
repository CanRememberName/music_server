package com.music.server.controller;

import com.music.server.model.ApiResponse;
import com.music.server.model.LoginRequest;
import com.music.server.model.User;
import com.music.server.repository.UserManageRepository;
import com.music.server.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.music.server.constants.RedisKeyConstant.getTokenRedisKey;
import static com.music.server.constants.RedisKeyConstant.getUserTokenRedisKey;

@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {

    @Value("${app.auth.token-ttl-days}")
    private int tokenTtlDays;

    private final StringRedisTemplate redisTemplate;

    private final UserManageRepository userManageRepository;

    public AuthController(StringRedisTemplate redisTemplate, UserManageRepository userManageRepository) {
         this.redisTemplate = redisTemplate;
         this.userManageRepository = userManageRepository;
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody LoginRequest request) {
        log.info("login request: {}", request);
        User user = userManageRepository.findByUsername(request.getUsername());
        if (!user.getUsername().equals(request.getUsername()) || !user.getPassword().equals(request.getPassword())) {
            return ApiResponse.error(1001, "Invalid username or password");
        }

        String token = UUID.randomUUID().toString();
        // Store simple user info in Redis (or just a flag)
        // Key: auth:token:{token} -> username
        String userKey = getUserTokenRedisKey(user.getUsername());
        redisTemplate.opsForValue().set(userKey, token, tokenTtlDays, TimeUnit.DAYS);

        String tokenKey = getTokenRedisKey(token);
        redisTemplate.opsForValue().set(tokenKey, JsonUtil.toJson(user), tokenTtlDays, TimeUnit.DAYS);

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
