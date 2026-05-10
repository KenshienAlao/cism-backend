package com.cism.backend.service.system;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.cism.backend.event.OrderEvent;

import com.cism.backend.dto.system.order.OrderItemResponse;
import com.cism.backend.dto.system.order.OrderRequest;
import com.cism.backend.dto.system.order.OrderResponse;
import com.cism.backend.exception.BadrequestException;
import com.cism.backend.model.admin.StallModel;
import com.cism.backend.model.stalls.ItemVariationsModel;
import com.cism.backend.model.stalls.StallIncomesModel;
import com.cism.backend.model.stalls.StallItemModel;
import com.cism.backend.model.system.order.OrderItemModel;
import com.cism.backend.model.system.order.OrderModel;
import com.cism.backend.model.system.review.CartModel;
import com.cism.backend.model.users.AuthModel;
import com.cism.backend.repository.stalls.ItemvariationsRepository;
import com.cism.backend.repository.admin.CreateStallIncomesRepository;
import com.cism.backend.repository.stalls.StallItemRepository;
import com.cism.backend.repository.system.CartRepository;
import com.cism.backend.repository.system.OrderItemRepository;
import com.cism.backend.repository.system.OrderRepository;
import com.cism.backend.repository.users.RegisterRepository;
import com.cism.backend.util.CurrentUserLicence;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import jakarta.transaction.Transactional;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private RegisterRepository registerRepository;

    @Autowired
    private StallItemRepository stallItemRepository;

    @Autowired
    private ItemvariationsRepository itemvariationsRepository;

    @Autowired
    private CreateStallIncomesRepository stallIncomesRepository;

    @Autowired
    private CurrentUserLicence currentUserLicence;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private static final BigDecimal DELIVERY_FEE_PER_ITEM = new BigDecimal("2.00");

    @Transactional
    public List<OrderResponse> addOrder(OrderRequest request) throws Exception {
        AuthModel user = getCurrentUser();
        List<OrderModel> createdOrders = new ArrayList<>();
        if (request.buyNowItem() != null) {
            OrderRequest.BuyNowItem buyItem = request.buyNowItem();
            StallItemModel stallItem = stallItemRepository.findById(buyItem.itemId())
                    .orElseThrow(() -> new BadrequestException("Item not found", "ITEM_NOT_FOUND"));

            ItemVariationsModel variation = null;
            if (buyItem.variationId() != null && buyItem.variationId() != 0) {
                variation = itemvariationsRepository.findById(buyItem.variationId())
                        .orElseThrow(() -> new BadrequestException("Variation not found", "VARIATION_NOT_FOUND"));
            }

            int availableStock = (variation != null)
                    ? (variation.getStock() != null ? variation.getStock() : 0)
                    : (stallItem.getStocks() != null ? stallItem.getStocks() : 0);
            if (availableStock < buyItem.quantity()) {
                throw new BadrequestException("Not enough stock for item: " + stallItem.getName(), "OUT_OF_STOCK");
            }

            BigDecimal price = (variation != null) ? variation.getPrice() : stallItem.getPrice();
            BigDecimal subtotal = price.multiply(new BigDecimal(buyItem.quantity()));
            BigDecimal deliveryFee = request.deliveryMethod().equalsIgnoreCase("DELIVER")
                    ? DELIVERY_FEE_PER_ITEM.multiply(new BigDecimal(buyItem.quantity()))
                    : BigDecimal.ZERO;

            OrderModel order = OrderModel.builder()
                    .user(user)
                    .stall(stallItem.getStall())
                    .orderCode(generateUniqueOrderCode(stallItem.getStall()))
                    .trackingToken(java.util.UUID.randomUUID().toString().replace("-", ""))
                    .subtotal(subtotal)
                    .deliveryFee(deliveryFee)
                    .totalAmount(subtotal.add(deliveryFee))
                    .deliveryMethod(request.deliveryMethod())
                    .paymentMethod(request.paymentMethod())
                    .status("PENDING")
                    .note(request.note())
                    .build();

            order = orderRepository.save(order);

            OrderItemModel orderItem = OrderItemModel.builder()
                    .order(order)
                    .stallItem(stallItem)
                    .variation(variation)
                    .quantity(buyItem.quantity())
                    .priceAtPurchase(price)
                    .build();
            orderItemRepository.save(orderItem);

            order.setOrderItems(List.of(orderItem));
            createdOrders.add(order);

            // Skip cart processing
        } else {
            List<CartModel> cartItems = cartRepository.findAllById(request.cartItemIds());

            if (cartItems.isEmpty()) {
                throw new BadrequestException("No items selected for checkout", "EMPTY_CHECKOUT");
            }

            // Verify ownership of all cart items
            for (CartModel cart : cartItems) {
                if (!cart.getUsers().getId().equals(user.getId())) {
                    throw new BadrequestException("Unauthorized cart item access", "UNAUTHORIZED");
                }
            }

            // Group by Stall
            Map<StallModel, List<CartModel>> stallGroups = new HashMap<>();
            for (CartModel cart : cartItems) {
                stallGroups.computeIfAbsent(cart.getStall(), k -> new ArrayList<>()).add(cart);
            }

            for (Map.Entry<StallModel, List<CartModel>> entry : stallGroups.entrySet()) {
                StallModel stall = entry.getKey();
                List<CartModel> items = entry.getValue();

                BigDecimal subtotal = BigDecimal.ZERO;
                int totalItemsCount = 0;

                for (CartModel cart : items) {
                    BigDecimal price = (cart.getVariation() != null) ? cart.getVariation().getPrice()
                            : cart.getStallItem().getPrice();
                    subtotal = subtotal.add(price.multiply(new BigDecimal(cart.getQuantity())));
                    totalItemsCount += cart.getQuantity();
                }

                BigDecimal deliveryFee = request.deliveryMethod().equalsIgnoreCase("DELIVER")
                        ? DELIVERY_FEE_PER_ITEM.multiply(new BigDecimal(totalItemsCount))
                        : BigDecimal.ZERO;

                OrderModel order = OrderModel.builder()
                        .user(user)
                        .stall(stall)
                        .orderCode(generateUniqueOrderCode(stall))
                        .trackingToken(java.util.UUID.randomUUID().toString().replace("-", "")
                                + java.util.UUID.randomUUID().toString().replace("-", ""))
                        .subtotal(subtotal)
                        .deliveryFee(deliveryFee)
                        .totalAmount(subtotal.add(deliveryFee))
                        .deliveryMethod(request.deliveryMethod())
                        .paymentMethod(request.paymentMethod())
                        .status("PENDING")
                        .note(request.note())
                        .build();

                order = orderRepository.save(order);

                List<OrderItemModel> orderItems = new ArrayList<>();
                for (CartModel cart : items) {
                    BigDecimal price = (cart.getVariation() != null) ? cart.getVariation().getPrice()
                            : cart.getStallItem().getPrice();

                    OrderItemModel orderItem = OrderItemModel.builder()
                            .order(order)
                            .stallItem(cart.getStallItem())
                            .variation(cart.getVariation())
                            .quantity(cart.getQuantity())
                            .priceAtPurchase(price)
                            .build();
                    orderItems.add(orderItemRepository.save(orderItem));

                    // Stock validation before checkout
                    int availableStock = (cart.getVariation() != null)
                            ? (cart.getVariation().getStock() != null ? cart.getVariation().getStock() : 0)
                            : (cart.getStallItem().getStocks() != null ? cart.getStallItem().getStocks() : 0);
                    if (availableStock < cart.getQuantity()) {
                        throw new BadrequestException("Not enough stock for item: " + cart.getStallItem().getName(),
                                "OUT_OF_STOCK");
                    }
                }
                order.setOrderItems(orderItems);
                createdOrders.add(order);
            }
            cartRepository.deleteAll(cartItems);
        }

        List<OrderResponse> responses = new ArrayList<>();
        for (OrderModel order : createdOrders) {
            OrderResponse response = mapToOrderResponse(order);
            responses.add(response);

            eventPublisher.publishEvent(new OrderEvent(this, response, "ORDER_CREATED", order.getStall().getId(),
                    order.getUser().getEmail()));
        }

        return responses;
    }

    public List<OrderResponse> getUserOrders() {
        return orderRepository.findByUser_IdAndDeletedByCustomerFalseOrderByCreatedAtDesc(getCurrentUser().getId())
                .stream()
                .map(this::mapToOrderResponse)
                .toList();
    }

    public OrderResponse getOrderById(String orderId) {
        OrderModel order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BadrequestException("Order not found", "ORDER_NOT_FOUND"));

        if (!order.getUser().getId().equals(getCurrentUser().getId())) {
            throw new BadrequestException("Unauthorized access to order", "UNAUTHORIZED");
        }

        return mapToOrderResponse(order);
    }

    @Transactional
    public OrderResponse cancelOrder(String orderId) {
        OrderModel order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BadrequestException("Order not found", "ORDER_NOT_FOUND"));

        if (!order.getUser().getId().equals(getCurrentUser().getId())) {
            throw new BadrequestException("Unauthorized access to order", "UNAUTHORIZED");
        }

        if (!order.getStatus().equals("PENDING")) {
            throw new BadrequestException("Only pending orders can be cancelled", "INVALID_STATUS");
        }

        order.setStatus("CANCELLED");

        // No need to restore stock here because stock is only deducted upon completion.

        OrderResponse response = mapToOrderResponse(orderRepository.save(order));

        eventPublisher.publishEvent(new OrderEvent(this, response, "ORDER_CANCELLED", order.getStall().getId(),
                order.getUser().getEmail()));

        return response;
    }

    @Transactional
    public List<OrderResponse> getStallOrders(Long stallId) {
        return orderRepository.findByStall_IdAndDeletedByStallFalseOrderByCreatedAtDesc(stallId)
                .stream()
                .map(this::mapToOrderResponse)
                .toList();
    }

    @Transactional
    public OrderResponse updateOrderStatus(String orderId, String status) {
        return updateOrderStatus(orderId, status, null);
    }

    @Transactional
    public OrderResponse updateOrderStatus(String orderId, String status, String reason) {
        OrderModel order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BadrequestException("Order not found", "ORDER_NOT_FOUND"));

        String currentStatus = order.getStatus().toUpperCase();
        String targetStatus = status.toUpperCase();

        if (currentStatus.equals(targetStatus)) {
            return mapToOrderResponse(order);
        }

        boolean isValid = false;
        switch (currentStatus) {
            case "PENDING":
                if (targetStatus.equals("PREPARING") || targetStatus.equals("CANCELLED"))
                    isValid = true;
                break;
            case "PREPARING":
                if (targetStatus.equals("READY"))
                    isValid = true;
                break;
            case "READY":
                if (targetStatus.equals("COMPLETED"))
                    isValid = true;
                break;
        }

        if (!isValid) {
            throw new BadrequestException("Invalid status transition from " + currentStatus + " to " + targetStatus,
                    "INVALID_TRANSITION");
        }

        if (targetStatus.equals("COMPLETED")) {
            for (OrderItemModel orderItem : order.getOrderItems()) {
                updateStockAndSold(orderItem.getStallItem(), orderItem.getVariation(), orderItem.getQuantity(), true);
            }

            // Record Income
            StallIncomesModel income = StallIncomesModel.builder()
                    .stall(order.getStall())
                    .income(order.getTotalAmount())
                    .earnedAt(java.time.Instant.now())
                    .createdAt(java.time.Instant.now())
                    .build();
            stallIncomesRepository.save(income);
        }

        order.setStatus(targetStatus);
        if ("CANCELLED".equals(targetStatus) && reason != null) {
            order.setCancelReason(reason);
        }

        OrderResponse response = mapToOrderResponse(orderRepository.save(order));

        eventPublisher.publishEvent(new OrderEvent(this, response, "ORDER_STATUS_CHANGED", order.getStall().getId(),
                order.getUser().getEmail()));

        return response;
    }

    @Transactional
    public void deleteOrder(String orderId) {
        OrderModel order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BadrequestException("Order not found", "ORDER_NOT_FOUND"));

        if (!order.getUser().getId().equals(getCurrentUser().getId())) {
            throw new BadrequestException("Unauthorized access to order", "UNAUTHORIZED");
        }

        if (!order.getStatus().equalsIgnoreCase("CANCELLED") && !order.getStatus().equalsIgnoreCase("COMPLETED")) {
            throw new BadrequestException("Only cancelled or completed orders can be deleted", "INVALID_STATUS");
        }

        order.setDeletedByCustomer(true);
        if (order.isDeletedByStall()) {
            orderRepository.delete(order);
        } else {
            orderRepository.save(order);
        }
    }

    @Transactional
    public void stallDeleteOrder(String orderId) {
        OrderModel order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BadrequestException("Order not found", "ORDER_NOT_FOUND"));

        // Logic for ensuring stall ownership is usually handled by the controller's
        // stallId parameter
        if (!order.getStatus().equalsIgnoreCase("CANCELLED") && !order.getStatus().equalsIgnoreCase("COMPLETED")) {
            throw new BadrequestException("Only cancelled or completed orders can be deleted", "INVALID_STATUS");
        }

        order.setDeletedByStall(true);
        if (order.isDeletedByCustomer()) {
            orderRepository.delete(order);
        } else {
            orderRepository.save(order);
        }
    }

    private OrderResponse mapToOrderResponse(OrderModel order) {
        List<OrderItemResponse> itemResponses = order.getOrderItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getId(),
                        item.getStallItem().getName(),
                        item.getVariation() != null ? item.getVariation().getName() : null,
                        item.getQuantity(),
                        item.getPriceAtPurchase(),
                        item.getStallItem().getImage(),
                        item.getStallItem().getId()))
                .toList();

        String stallName = (order.getStall().getUserList() != null && !order.getStall().getUserList().isEmpty())
                ? order.getStall().getUserList().get(0).getName()
                : "Stall " + order.getStall().getLicence();

        String stallImage = (order.getStall().getUserList() != null && !order.getStall().getUserList().isEmpty())
                ? order.getStall().getUserList().get(0).getImage()
                : null;

        return new OrderResponse(
                order.getId(),
                order.getOrderCode(),
                order.getSubtotal(),
                order.getDeliveryFee(),
                order.getTotalAmount(),
                order.getDeliveryMethod(),
                order.getPaymentMethod(),
                order.getStatus(),
                order.getNote(),
                order.getCancelReason(),
                order.getCreatedAt(),
                stallName,
                stallImage,
                order.getStall().getId(),
                itemResponses);
    }

    private void updateStockAndSold(StallItemModel item, ItemVariationsModel variation, int quantity,
            boolean isDeducting) {
        int factor = isDeducting ? 1 : -1;

        if (variation != null) {
            if (variation.getStock() != null) {
                variation.setStock(Math.max(0, variation.getStock() - (quantity * factor)));
                itemvariationsRepository.save(variation);
            }
        } else {
            if (item.getStocks() != null) {
                item.setStocks(Math.max(0, item.getStocks() - (quantity * factor)));
            }
        }

        if (item.getSold() != null) {
            item.setSold(Math.max(0, item.getSold() + (quantity * factor)));
        }
        stallItemRepository.save(item);

        Map<String, Object> stockUpdate = new HashMap<>();
        stockUpdate.put("itemId", item.getId());
        stockUpdate.put("variationId", variation != null ? variation.getId() : null);
        stockUpdate.put("newStock", variation != null ? variation.getStock() : item.getStocks());
        messagingTemplate.convertAndSend("/topic/inventory", (Object) stockUpdate);
    }

    private AuthModel getCurrentUser() {
        String email = currentUserLicence.getCurrentUserEmail();
        return registerRepository.findByEmail(email)
                .orElseThrow(() -> new BadrequestException("User not found", "USER_NOT_FOUND"));
    }

    private String generateUniqueOrderCode(StallModel stall) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        java.util.Random random = new java.util.Random();
        String code;
        String stallPrefix = stall.getLicence().toUpperCase().replaceAll("[^A-Z0-9]", "");
        if (stallPrefix.length() > 5) {
            stallPrefix = stallPrefix.substring(0, 5);
        }
        do {
            StringBuilder sb = new StringBuilder("CISM-");
            sb.append(stallPrefix).append("-");
            for (int i = 0; i < 5; i++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }
            code = sb.toString();
        } while (orderRepository.existsByOrderCode(code));
        return code;
    }
}
