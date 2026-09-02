package com.ecommerce.order.controller;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import java.util.Map;

@RestController
@RequestMapping
public class MessageController {

    @GetMapping("/message")
    @RateLimiter(name = "rateBreaker", fallbackMethod = "getMessageFallback")
    public ResponseEntity<Map<String, String>> getMessage() {
        return ResponseEntity.ok(Map.of("message", "Hello Message"));
    }

    public ResponseEntity<Map<String, String>> getMessageFallback(Throwable throwable) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of(
                        "message", "Rate limit exceeded. Please try again later.",
                        "error", throwable.getClass().getSimpleName()
                ));
    }
}