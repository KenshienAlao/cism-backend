package com.cism.backend.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;

@Component
public class OrderEventListener {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderEvent(OrderEvent event) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", event.getType());
        payload.put("data", event.getOrderResponse());

        messagingTemplate.convertAndSend(
                "/topic/stall/" + event.getStallId() + "/orders",
                (Object) payload);

        messagingTemplate.convertAndSendToUser(
                event.getUserEmail(),
                "/queue/orders",
                payload);
    }
}
