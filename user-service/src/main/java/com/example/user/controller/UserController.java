package com.example.user.controller;

import com.example.common.dto.UserDTO;
import com.example.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    @GetMapping("/{id}")
    public Result<UserDTO> getUser(@PathVariable("id") Long id) {
        // Mock DB
        UserDTO user = new UserDTO();
        user.setId(id);
        user.setUsername("User-" + id);
        user.setEmail("user" + id + "@example.com");
        return Result.success(user);
    }
}
