package com.cism.backend.event;

import com.cism.backend.dto.system.order.OrderResponse;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class OrderEvent extends ApplicationEvent {
    private final OrderResponse orderResponse;
    private final String type;
    private final Long stallId;
    private final String userEmail;

    public OrderEvent(Object source, OrderResponse orderResponse, String type, Long stallId, String userEmail) {
        super(source);
        this.orderResponse = orderResponse;
        this.type = type;
        this.stallId = stallId;
        this.userEmail = userEmail;
    }
}
