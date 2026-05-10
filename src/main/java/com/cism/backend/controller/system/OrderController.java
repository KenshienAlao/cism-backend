package com.cism.backend.controller.system;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cism.backend.dto.common.Api;
import com.cism.backend.dto.system.order.OrderRequest;
import com.cism.backend.dto.system.order.OrderResponse;
import com.cism.backend.service.system.OrderService;

@RestController
@RequestMapping("/api/customer/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/add-order")
    public ResponseEntity<Api<List<OrderResponse>>> addOrder(@RequestBody OrderRequest request) throws Exception {
        List<OrderResponse> success = orderService.addOrder(request);
        return ResponseEntity.ok(Api.ok("Order placed successfully", "ORDER_PLACED", success));
    }

    @GetMapping("/my-orders")
    public ResponseEntity<Api<List<OrderResponse>>> getMyOrders() {
        List<OrderResponse> success = orderService.getUserOrders();
        return ResponseEntity.ok(Api.ok("Orders retrieved successfully", "ORDERS_RETRIEVED", success));
    }

    @PostMapping("/cancel-order/{id}")
    public ResponseEntity<Api<OrderResponse>> cancelOrder(
            @PathVariable String id,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String reason) {
        OrderResponse success = orderService.cancelOrder(id, reason);
        return ResponseEntity.ok(Api.ok("Order cancelled successfully", "ORDER_CANCELLED", success));
    }

    @GetMapping("/my-orders/{id}")
    public ResponseEntity<Api<OrderResponse>> getOrderById(@PathVariable String id) {
        OrderResponse success = orderService.getOrderById(id);
        return ResponseEntity.ok(Api.ok("Order retrieved successfully", "ORDER_RETRIEVED", success));
    }

    @PostMapping("/receive-order/{id}")
    public ResponseEntity<Api<OrderResponse>> receiveOrder(@PathVariable String id) {
        OrderResponse success = orderService.updateOrderStatus(id, "COMPLETED");
        return ResponseEntity.ok(Api.ok("Order marked as received", "ORDER_RECEIVED", success));
    }

    @PostMapping("/delete-order/{id}")
    public ResponseEntity<Api<String>> deleteOrder(@PathVariable String id) {
        orderService.deleteOrder(id);
        return ResponseEntity.ok(Api.ok("Order deleted successfully", "ORDER_DELETED", "SUCCESS"));
    }

}
