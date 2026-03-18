package com.example.order.controller;

import com.example.common.dto.UserDTO;
import com.example.common.result.Result;
import com.example.order.client.UserClient;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final UserClient userClient;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @PostMapping("/create")
    public Result<String> createOrder(@RequestParam("userId") Long userId, @RequestParam("product") String product) {
        // 1. RPC call to User Service (Sync)
        Result<UserDTO> userResult = userClient.getUser(userId);
        if (userResult.getCode() != 200 || userResult.getData() == null) {
            return Result.failed("User not found");
        }

        // 2. Create Order Logic (Mock)
        String orderId = "ORD-" + System.currentTimeMillis();
        String message = "Order " + orderId + " created for user " + userResult.getData().getUsername();

        // 3. Send event to Kafka (Async)
        kafkaTemplate.send("order-created", message);

        return Result.success(orderId);
    }
}

