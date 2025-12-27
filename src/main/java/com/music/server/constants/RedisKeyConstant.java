package com.music.server.constants;

import com.google.common.collect.Lists;

import java.util.List;

public class RedisKeyConstant {

    public static final String USER_INFO = "user_key:user_name:";

    public static final String TOKEN = "auth:token:";

    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_USER = "ROLE_USER";

    public static final List<String> ONLY_ADMIN_URIS = Lists.newArrayList("/api/v1/users/list");


    public static String getUserTokenRedisKey(String username) {
        return USER_INFO + username;
    }

    public static String getTokenRedisKey(String token) {
        return TOKEN + token;
    }
}
