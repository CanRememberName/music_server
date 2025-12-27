package com.music.server.controller;


import com.google.common.collect.Lists;
import com.music.server.model.*;
import com.music.server.repository.UserManageRepository;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.music.server.constants.RedisKeyConstant.*;

@RestController
@RequestMapping("/users")
@Slf4j
public class UserManageController {

    @Value("${app.auth.username}")
    private String adminUsername;

    @Value("${app.auth.password}")
    private String adminPassword;

    private final StringRedisTemplate redisTemplate;

    private final UserManageRepository userManageRepository;

    public UserManageController(StringRedisTemplate redisTemplate, UserManageRepository userManageRepository) {
        this.redisTemplate = redisTemplate;
        this.userManageRepository = userManageRepository;
    }

    @PostMapping("/create")
    public ApiResponse<User> create(@RequestBody CreateUserRequest request) {
        if (Objects.isNull(request) || StringUtils.isEmpty(request.getUsername()) || StringUtils.isEmpty(request.getPassword())) {
            return ApiResponse.error(1001, "Invalid username or password");
        }
        // 持久化数据
        User user = userManageRepository.save(new User(UUID.randomUUID().toString(), request.getUsername(), request.getPassword(), Lists.newArrayList(ROLE_USER)));
        return ApiResponse.success(user);
    }


    @PostMapping("/delete")
    public ApiResponse<User> delete(@RequestBody DeleteUserRequest request) {
        if (Objects.isNull(request) || StringUtils.isEmpty(request.getId())) {
            return ApiResponse.error(1001, "Invalid request");
        }
        User existUser = userManageRepository.findById(request.getId()).orElse(null);
        if (Objects.isNull(existUser)) {
            return ApiResponse.error(1001, "User does not exist");
        }
        // 持久化数据
        userManageRepository.deleteById(request.getId());
        // 强制下线
        String token = redisTemplate.opsForValue().get(getUserTokenRedisKey(existUser.getUsername()));
        redisTemplate.delete(getUserTokenRedisKey(existUser.getUsername()));
        redisTemplate.delete(getTokenRedisKey(token));
        return ApiResponse.success();
    }

    @PostMapping("/kick")
    public ApiResponse<User> kick(@RequestBody KickUserRequest request) {
        if (Objects.isNull(request) || StringUtils.isEmpty(request.getId())) {
            return ApiResponse.error(1001, "Invalid request");
        }
        User existUser = userManageRepository.findById(request.getId()).orElse(null);
        if (Objects.isNull(existUser)) {
            return ApiResponse.error(1001, "User does not exist");
        }
        // 强制下线
        String token = redisTemplate.opsForValue().get(getUserTokenRedisKey(existUser.getUsername()));
        redisTemplate.delete(getUserTokenRedisKey(existUser.getUsername()));
        redisTemplate.delete(getTokenRedisKey(token));
        return ApiResponse.success();
    }

    @GetMapping("/list")
    public ApiResponse<List<User>> list() {
        return ApiResponse.success(userManageRepository.findAll());

//        return ApiResponse.success(userManageRepository.findAll().stream().filter(item -> !adminUsername.equals(item.getUsername())).collect(Collectors.toList()));
    }
}
