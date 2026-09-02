package com.ecommerce.notification_service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class OrderEventConsumer {

    /**
     * Listens to the queue defined in application.yml.
     * Jackson2JsonMessageConverter will automatically deserialize
     * the incoming JSON payload into a Map.
     */
    @RabbitListener(queues = "${rabbitmq.queue.name:order.queue}")
    public void consumeOrderEvent(OrderEventDto event) {
        log.info("Received full OrderCreateEvent for Order ID: {}", event.getOrderId());
        log.info("User: {}, Total Amount: {}, Items Count: {}",
                event.getUserId(), event.getTotalAmount(), event.getItems().size());

        event.getItems().forEach(item ->
                log.info(" - Product ID: {}, Quantity: {}, Price: {}",
                        item.getProductId(), item.getQuantity(), item.getPrice())
        );

        // Process your business logic here (e.g., notify notification-service, update tracking, etc.)
    }
}