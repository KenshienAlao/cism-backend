package com.cism.backend.controller.stall;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cism.backend.dto.common.Api;
import com.cism.backend.dto.system.order.OrderResponse;
import com.cism.backend.service.system.OrderService;
import com.cism.backend.util.CurrentUserLicence;

@RestController
@RequestMapping("/api/stall/order")
public class StallOrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CurrentUserLicence currentUserLicence;

    @GetMapping("/my-orders")
    public ResponseEntity<Api<List<OrderResponse>>> getMyStallOrders() {
        Long stallId = currentUserLicence.getStall().getId();
        List<OrderResponse> success = orderService.getStallOrders(stallId);
        return ResponseEntity.ok(Api.ok("Stall orders retrieved successfully", "STALL_ORDERS_RETRIEVED", success));
    }

    @PostMapping("/update-status/{id}")
    public ResponseEntity<Api<OrderResponse>> updateOrderStatus(
            @PathVariable String id,
            @RequestParam String status,
            @RequestParam(required = false) String reason) {
        OrderResponse success = orderService.updateOrderStatus(id, status, reason);
        return ResponseEntity.ok(Api.ok("Order status updated successfully", "ORDER_STATUS_UPDATED", success));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{id}")
    public ResponseEntity<Api<String>> deleteOrder(@PathVariable String id) {
        orderService.stallDeleteOrder(id);
        return ResponseEntity.ok(Api.ok("Order deleted from stall view", "ORDER_DELETED", null));
    }
}
