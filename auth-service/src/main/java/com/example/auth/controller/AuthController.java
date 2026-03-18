package com.example.auth.controller;

import com.example.common.result.Result;
import com.example.common.util.JwtUtil;
import com.example.common.util.TokenCache;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public Result<String> login(@RequestParam("username") String username, @RequestParam("password") String password) {
        // Mock authentication
        if ("admin".equals(username) && "password".equals(password)) {
            String token = jwtUtil.generateToken(username);
            TokenCache.put(token); // 缓存token
            return Result.success(token);
        }
        return Result.failed("Invalid credentials");
    }
}
