package com.example.user.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UserKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserKafkaConsumer.class);

    @KafkaListener(topics = "order-created", groupId = "user-group")
    public void onOrderCreated(String message) {
        log.info("Received order created event: {}", message);
        // Process event (e.g., update user stats)
    }
}
