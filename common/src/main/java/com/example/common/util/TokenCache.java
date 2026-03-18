package com.example.common.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TokenCache {
    private static final Map<String, Long> TOKEN_MAP = new ConcurrentHashMap<>();

    public static void put(String token) {
        TOKEN_MAP.put(token, System.currentTimeMillis());
    }

    public static boolean contains(String token) {
        return TOKEN_MAP.containsKey(token);
    }

    public static void remove(String token) {
        TOKEN_MAP.remove(token);
    }
}

