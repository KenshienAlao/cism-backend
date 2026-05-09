package com.cism.backend.service.system;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cism.backend.dto.system.order.OrderItemResponse;
import com.cism.backend.dto.system.order.OrderRequest;
import com.cism.backend.dto.system.order.OrderResponse;
import com.cism.backend.exception.BadrequestException;
import com.cism.backend.model.admin.StallModel;
import com.cism.backend.model.stalls.ItemVariationsModel;
import com.cism.backend.model.stalls.StallItemModel;
import com.cism.backend.model.system.order.OrderItemModel;
import com.cism.backend.model.system.order.OrderModel;
import com.cism.backend.model.system.review.CartModel;
import com.cism.backend.model.users.AuthModel;
import com.cism.backend.repository.stalls.ItemvariationsRepository;
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
    private CurrentUserLicence currentUserLicence;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private static final BigDecimal DELIVERY_FEE_PER_ITEM = new BigDecimal("2.00");

    @Transactional
    public List<OrderResponse> addOrder(OrderRequest request) throws Exception {
        AuthModel user = getCurrentUser();
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

        List<OrderModel> createdOrders = new ArrayList<>();

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

            BigDecimal deliveryFee = request.deliveryMethod().equalsIgnoreCase("DELIVERY")
                    ? DELIVERY_FEE_PER_ITEM.multiply(new BigDecimal(totalItemsCount))
                    : BigDecimal.ZERO;

            OrderModel order = OrderModel.builder()
                    .user(user)
                    .stall(stall)
                    .receipt(generateUniqueReceiptCode())
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

                updateStockAndSold(cart.getStallItem(), cart.getVariation(), cart.getQuantity(), true);
            }
            order.setOrderItems(orderItems);
            createdOrders.add(order);
        }

        cartRepository.deleteAll(cartItems);

        List<OrderResponse> responses = new ArrayList<>();
        for (OrderModel order : createdOrders) {
            OrderResponse response = mapToOrderResponse(order);
            responses.add(response);

            messagingTemplate.convertAndSendToUser(
                    order.getUser().getEmail(),
                    "/queue/orders",
                    response);

            messagingTemplate.convertAndSendToUser(
                    order.getStall().getLicence(),
                    "/queue/orders",
                    response);
        }

        return responses;
    }

    public List<OrderResponse> getUserOrders() {
        return orderRepository.findByUser_IdOrderByCreatedAtDesc(getCurrentUser().getId())
                .stream()
                .map(this::mapToOrderResponse)
                .toList();
    }

    public OrderResponse getOrderById(Long orderId) {
        OrderModel order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BadrequestException("Order not found", "ORDER_NOT_FOUND"));

        if (!order.getUser().getId().equals(getCurrentUser().getId())) {
            throw new BadrequestException("Unauthorized access to order", "UNAUTHORIZED");
        }

        return mapToOrderResponse(order);
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId) {
        OrderModel order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BadrequestException("Order not found", "ORDER_NOT_FOUND"));

        if (!order.getUser().getId().equals(getCurrentUser().getId())) {
            throw new BadrequestException("Unauthorized access to order", "UNAUTHORIZED");
        }

        if (!order.getStatus().equals("PENDING")) {
            throw new BadrequestException("Only pending orders can be cancelled", "INVALID_STATUS");
        }

        order.setStatus("CANCELLED");

        for (OrderItemModel orderItem : order.getOrderItems()) {
            updateStockAndSold(orderItem.getStallItem(), orderItem.getVariation(), orderItem.getQuantity(), false);
        }

        OrderResponse response = mapToOrderResponse(orderRepository.save(order));

        // Notify both customer and stall owner
        messagingTemplate.convertAndSendToUser(
                order.getUser().getEmail(),
                "/queue/orders",
                response);

        messagingTemplate.convertAndSendToUser(
                order.getStall().getLicence(),
                "/queue/orders",
                response);

        return response;
    }

    public List<OrderResponse> getStallOrders(Long stallId) {
        return orderRepository.findByStall_IdOrderByCreatedAtDesc(stallId)
                .stream()
                .map(this::mapToOrderResponse)
                .toList();
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, String status) {
        OrderModel order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BadrequestException("Order not found", "ORDER_NOT_FOUND"));

        List<String> validStatus = List.of("PENDING", "PREPARING", "READY", "COMPLETED", "CANCELLED");
        if (!validStatus.contains(status.toUpperCase())) {
            throw new BadrequestException("Invalid status", "INVALID_STATUS");
        }

        order.setStatus(status.toUpperCase());
        OrderResponse response = mapToOrderResponse(orderRepository.save(order));

        messagingTemplate.convertAndSendToUser(
                order.getUser().getEmail(),
                "/queue/orders",
                response);

        messagingTemplate.convertAndSendToUser(
                order.getStall().getLicence(),
                "/queue/orders",
                response);

        return response;
    }

    @Transactional
    public void deleteOrder(Long orderId) {
        OrderModel order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BadrequestException("Order not found", "ORDER_NOT_FOUND"));

        if (!order.getUser().getId().equals(getCurrentUser().getId())) {
            throw new BadrequestException("Unauthorized access to order", "UNAUTHORIZED");
        }

        if (!order.getStatus().equalsIgnoreCase("CANCELLED")) {
            throw new BadrequestException("Only cancelled orders can be deleted", "INVALID_STATUS");
        }

        orderRepository.delete(order);
    }

    private OrderResponse mapToOrderResponse(OrderModel order) {
        List<OrderItemResponse> itemResponses = order.getOrderItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getId(),
                        item.getStallItem().getName(),
                        item.getVariation() != null ? item.getVariation().getName() : null,
                        item.getQuantity(),
                        item.getPriceAtPurchase(),
                        item.getStallItem().getImage()))
                .toList();

        String stallName = (order.getStall().getUserList() != null && !order.getStall().getUserList().isEmpty())
                ? order.getStall().getUserList().get(0).getName()
                : "Stall " + order.getStall().getLicence();

        return new OrderResponse(
                order.getId(),
                order.getReceipt(),
                order.getSubtotal(),
                order.getDeliveryFee(),
                order.getTotalAmount(),
                order.getDeliveryMethod(),
                order.getPaymentMethod(),
                order.getStatus(),
                order.getNote(),
                order.getCreatedAt(),
                stallName,
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

    private String generateUniqueReceiptCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        java.util.Random random = new java.util.Random();
        String code;
        do {
            StringBuilder sb = new StringBuilder("#");
            for (int i = 0; i < 5; i++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }
            code = sb.toString();
        } while (orderRepository.existsByReceipt(code));
        return code;
    }
}
