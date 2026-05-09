package com.cism.backend.service.system.chat;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.cism.backend.dto.system.chat.ChatRequest;
import com.cism.backend.dto.system.chat.ChatResponse;
import com.cism.backend.dto.system.chat.ChatThreadResponse;
import com.cism.backend.dto.system.chat.CustomerSearchResponse;
import com.cism.backend.exception.BadrequestException;
import com.cism.backend.model.admin.StallModel;
import com.cism.backend.model.system.chat.ChatModel;
import com.cism.backend.model.system.order.OrderModel;
import com.cism.backend.model.users.AuthModel;
import com.cism.backend.repository.admin.CreateStallRepository;
import com.cism.backend.repository.system.OrderRepository;
import com.cism.backend.repository.system.chat.ChatRepository;
import com.cism.backend.repository.users.RegisterRepository;
import com.cism.backend.util.CurrentUserLicence;
import com.cism.backend.model.system.chat.ConversationModel;
import com.cism.backend.repository.system.chat.ConversationRepository;

@Service
public class ChatService {

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private RegisterRepository registerRepository;

    @Autowired
    private CreateStallRepository stallRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CurrentUserLicence currentUserLicence;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public ChatResponse sendMessage(ChatRequest request) {
        String currentUsername = currentUserLicence.getCurrentUserEmail();

        StallModel stall = stallRepository.findById(request.stallId())
                .orElseThrow(() -> new BadrequestException("Stall not found", "STALL_NOT_FOUND"));

        boolean isStallOwner = stall.getLicence().equals(currentUsername);

        AuthModel customer;
        AuthModel sender = null;

        if (isStallOwner) {
            if (request.customerId() == null) {
                throw new BadrequestException("Customer ID is required when stall owner sends a message",
                        "CUSTOMER_REQUIRED");
            }
            customer = registerRepository.findById(request.customerId())
                    .orElseThrow(() -> new BadrequestException("Customer not found", "CUSTOMER_NOT_FOUND"));
        } else {
            customer = registerRepository.findByEmail(currentUsername)
                    .orElseThrow(() -> new BadrequestException("User not found", "USER_NOT_FOUND"));
            sender = customer;
        }

        // Find or create conversation
        ConversationModel conversation = conversationRepository
                .findByCustomer_IdAndStall_Id(customer.getId(), stall.getId())
                .orElseGet(() -> {
                    ConversationModel newConv = ConversationModel.builder()
                            .customer(customer)
                            .stall(stall)
                            .build();
                    return conversationRepository.save(newConv);
                });

        ChatModel chat = ChatModel.builder()
                .conversation(conversation)
                .sender(sender)
                .stall(stall)
                .customer(customer)
                .content(request.content())
                .readByCustomer(!isStallOwner) // If customer sends, they've read it
                .readByStall(isStallOwner) // If stall sends, they've read it
                .build();

        chat = chatRepository.save(chat);

        // Update last message time
        conversation.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        ChatResponse response = mapToResponse(chat);

        // Broadcast to both parties
        messagingTemplate.convertAndSendToUser(customer.getEmail(), "/queue/chat", response);
        messagingTemplate.convertAndSendToUser(stall.getLicence(), "/queue/chat", response);

        return response;
    }

    public List<ChatResponse> getChatHistory(Long stallId, Long customerId, String conversationId) {
        String currentUsername = currentUserLicence.getCurrentUserEmail();

        if (conversationId != null) {
            return chatRepository.findByConversation_ConversationIdOrderByCreatedAtAsc(conversationId)
                    .stream()
                    .filter(c -> {
                        StallModel stall = c.getStall();
                        boolean isOwner = stall.getLicence().equals(currentUsername);
                        return isOwner ? !c.isDeletedForStall() : !c.isDeletedForCustomer();
                    })
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }

        // Fallback to legacy ID-based fetch
        StallModel stall = stallRepository.findById(stallId)
                .orElseThrow(() -> new BadrequestException("Stall not found", "STALL_NOT_FOUND"));

        boolean isOwner = stall.getLicence().equals(currentUsername);

        if (isOwner) {
            return chatRepository.findByStall_IdAndCustomer_IdOrderByCreatedAtAsc(stallId, customerId)
                    .stream()
                    .filter(c -> !c.isDeletedForStall())
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        } else {
            AuthModel user = registerRepository.findByEmail(currentUsername)
                    .orElseThrow(() -> new BadrequestException("User not found", "USER_NOT_FOUND"));
            return chatRepository.findByStall_IdAndCustomer_IdOrderByCreatedAtAsc(stallId, user.getId())
                    .stream()
                    .filter(c -> !c.isDeletedForCustomer())
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }
    }

    public void markMessagesAsRead(Long stallId, Long customerId) {
        String currentUsername = currentUserLicence.getCurrentUserEmail();

        StallModel stall = stallRepository.findById(stallId)
                .orElseThrow(() -> new BadrequestException("Stall not found", "STALL_NOT_FOUND"));

        boolean isOwner = stall.getLicence().equals(currentUsername);
        List<ChatModel> unreadMessages;

        if (isOwner) {
            unreadMessages = chatRepository.findByStall_IdAndCustomer_IdOrderByCreatedAtAsc(stallId, customerId)
                    .stream()
                    .filter(c -> !c.isReadByStall())
                    .collect(Collectors.toList());
            unreadMessages.forEach(c -> c.setReadByStall(true));
        } else {
            AuthModel user = registerRepository.findByEmail(currentUsername)
                    .orElseThrow(() -> new BadrequestException("User not found", "USER_NOT_FOUND"));
            unreadMessages = chatRepository.findByStall_IdAndCustomer_IdOrderByCreatedAtAsc(stallId, user.getId())
                    .stream()
                    .filter(c -> !c.isReadByCustomer())
                    .collect(Collectors.toList());
            unreadMessages.forEach(c -> c.setReadByCustomer(true));
        }

        if (!unreadMessages.isEmpty()) {
            chatRepository.saveAll(unreadMessages);

            // Broadcast read receipt
            Map<String, Object> readEvent = new HashMap<>();
            readEvent.put("type", "READ_RECEIPT");
            readEvent.put("stallId", stallId);
            readEvent.put("customerId", customerId != null ? customerId : unreadMessages.get(0).getCustomer().getId());
            readEvent.put("sentByStall", isOwner);

            messagingTemplate.convertAndSendToUser(unreadMessages.get(0).getCustomer().getEmail(), "/queue/chat",
                    readEvent);
            messagingTemplate.convertAndSendToUser(stall.getLicence(), "/queue/chat", readEvent);
        }
    }

    public List<ChatThreadResponse> getChatThreads() {
        String currentUsername = currentUserLicence.getCurrentUserEmail();
        List<ChatThreadResponse> threads = new ArrayList<>();

        boolean isStallOwner = stallRepository.findByLicence(currentUsername).isPresent();

        if (isStallOwner) {
            StallModel stall = stallRepository.findByLicence(currentUsername).get();
            List<ConversationModel> conversations = conversationRepository.findByStall_Id(stall.getId());
            for (ConversationModel conv : conversations) {
                ChatModel latest = chatRepository
                        .findByStall_IdAndCustomer_IdOrderByCreatedAtAsc(stall.getId(), conv.getCustomer().getId())
                        .stream().reduce((first, second) -> second).orElse(null);

                if (latest != null) {
                    boolean unread = !latest.isReadByStall();
                    threads.add(mapToThreadResponse(conv, latest, unread));
                }
            }
        } else {
            AuthModel user = registerRepository.findByEmail(currentUsername).get();
            List<ConversationModel> conversations = conversationRepository.findByCustomer_Id(user.getId());
            for (ConversationModel conv : conversations) {
                ChatModel latest = chatRepository
                        .findByStall_IdAndCustomer_IdOrderByCreatedAtAsc(conv.getStall().getId(), user.getId())
                        .stream().reduce((first, second) -> second).orElse(null);

                if (latest != null) {
                    boolean unread = !latest.isReadByCustomer();
                    threads.add(mapToThreadResponse(conv, latest, unread));
                }
            }
        }

        threads.sort((a, b) -> b.lastMessageAt().compareTo(a.lastMessageAt()));
        return threads;
    }

    private ChatThreadResponse mapToThreadResponse(ConversationModel conv, ChatModel latest, boolean unread) {
        String stallName = (conv.getStall().getUserList() != null && !conv.getStall().getUserList().isEmpty())
                ? conv.getStall().getUserList().get(0).getName()
                : "Stall " + conv.getStall().getLicence();

        String stallImage = (conv.getStall().getUserList() != null && !conv.getStall().getUserList().isEmpty())
                ? conv.getStall().getUserList().get(0).getImage()
                : null;

        return new ChatThreadResponse(
                conv.getConversationId(),
                conv.getStall().getId(),
                stallName,
                stallImage,
                conv.getCustomer().getId(),
                conv.getCustomer().getClientName(),
                conv.getCustomer().getAvatar(),
                latest.getContent(),
                conv.getLastMessageAt() != null ? conv.getLastMessageAt() : latest.getCreatedAt(),
                unread);
    }

    public List<CustomerSearchResponse> searchCustomers(String query) {
        String currentUsername = currentUserLicence.getCurrentUserEmail();
        StallModel stall = stallRepository.findByLicence(currentUsername)
                .orElseThrow(() -> new BadrequestException("You are not a stall owner", "UNAUTHORIZED"));

        List<ChatModel> stallChats = chatRepository.findByStall_IdOrderByCreatedAtDesc(stall.getId());
        Map<Long, ChatModel> latestChatPerCustomer = new HashMap<>();
        for (ChatModel chat : stallChats) {
            latestChatPerCustomer.putIfAbsent(chat.getCustomer().getId(), chat);
        }

        List<OrderModel> stallOrders = orderRepository.findByStall_IdOrderByCreatedAtDesc(stall.getId());
        Map<Long, OrderModel> latestOrderPerCustomer = new HashMap<>();
        for (OrderModel order : stallOrders) {
            latestOrderPerCustomer.putIfAbsent(order.getUser().getId(), order);
        }

        List<Long> allCustomerIds = new ArrayList<>(latestChatPerCustomer.keySet());
        for (Long orderCustomerId : latestOrderPerCustomer.keySet()) {
            if (!allCustomerIds.contains(orderCustomerId)) {
                allCustomerIds.add(orderCustomerId);
            }
        }

        List<CustomerSearchResponse> results = new ArrayList<>();
        for (Long customerId : allCustomerIds) {
            AuthModel customer = registerRepository.findById(customerId).orElse(null);
            if (customer == null)
                continue;

            if (query != null && !query.isEmpty()
                    && !customer.getClientName().toLowerCase().contains(query.toLowerCase())) {
                continue;
            }

            ChatModel latestChat = latestChatPerCustomer.get(customerId);
            OrderModel latestOrder = latestOrderPerCustomer.get(customerId);

            String lastMsg = latestChat != null ? latestChat.getContent() : "Ordered " + latestOrder.getReceipt();
            LocalDateTime lastTime = latestChat != null ? latestChat.getCreatedAt()
                    : LocalDateTime.ofInstant(latestOrder.getCreatedAt(), ZoneId.systemDefault());

            results.add(new CustomerSearchResponse(
                    customer.getId(),
                    customer.getClientName(),
                    customer.getAvatar(),
                    lastMsg,
                    lastTime,
                    latestChat != null));
        }

        results.sort((a, b) -> b.lastMessageAt().compareTo(a.lastMessageAt()));
        return results;
    }

    public Map<String, Object> getPresence(String type, Long id) {
        Map<String, Object> presence = new HashMap<>();
        presence.put("id", id);
        presence.put("userType", type);
        presence.put("isOnline", false);
        presence.put("lastSeenAt", null);

        if ("STALL".equals(type)) {
            stallRepository.findById(id).ifPresent(stall -> {
                presence.put("isOnline", stall.isOnline());
                presence.put("lastSeenAt", stall.getLastSeenAt());
            });
        } else {
            registerRepository.findById(id).ifPresent(user -> {
                presence.put("isOnline", user.isOnline());
                presence.put("lastSeenAt", user.getLastSeenAt());
            });
        }
        return presence;
    }

    public void updatePresence(String email, boolean isOnline) {
        AuthModel user = registerRepository.findByEmail(email).orElse(null);
        if (user != null) {
            user.setOnline(isOnline);
            user.setLastSeenAt(LocalDateTime.now());
            registerRepository.save(user);
            broadcastStatus(user.getId(), "CLIENT", isOnline);
            return;
        }

        StallModel stall = stallRepository.findByLicence(email).orElse(null);
        if (stall != null) {
            stall.setOnline(isOnline);
            stall.setLastSeenAt(LocalDateTime.now());
            stallRepository.save(stall);
            broadcastStatus(stall.getId(), "STALL", isOnline);
        }
    }

    private void broadcastStatus(Long id, String type, boolean isOnline) {
        Map<String, Object> statusEvent = new HashMap<>();
        statusEvent.put("type", "PRESENCE_UPDATE");
        statusEvent.put("id", id);
        statusEvent.put("userType", type);
        statusEvent.put("isOnline", isOnline);
        statusEvent.put("lastSeenAt", LocalDateTime.now());

        messagingTemplate.convertAndSend("/topic/presence", (Object) statusEvent);
    }

    public ChatResponse deleteMessage(Long messageId, boolean forMe) {
        String currentUsername = currentUserLicence.getCurrentUserEmail();
        ChatModel chat = chatRepository.findById(messageId)
                .orElseThrow(() -> new BadrequestException("Message not found", "MESSAGE_NOT_FOUND"));

        boolean isStallOwner = chat.getStall().getLicence().equals(currentUsername);

        if (forMe) {
            if (isStallOwner) {
                chat.setDeletedForStall(true);
            } else {
                chat.setDeletedForCustomer(true);
            }
        } else {
            chat.setDeleted(true);
        }

        chat = chatRepository.save(chat);
        ChatResponse response = mapToResponse(chat);

        if (!forMe) {
            Map<String, Object> deleteEvent = new HashMap<>();
            deleteEvent.put("type", "MESSAGE_DELETED");
            deleteEvent.put("messageId", messageId);
            deleteEvent.put("conversationId",
                    chat.getConversation() != null ? chat.getConversation().getConversationId() : null);

            messagingTemplate.convertAndSendToUser(chat.getCustomer().getEmail(), "/queue/chat", deleteEvent);
            messagingTemplate.convertAndSendToUser(chat.getStall().getLicence(), "/queue/chat", deleteEvent);
        }

        return response;
    }

    private ChatResponse mapToResponse(ChatModel chat) {
        Long senderId = chat.getSender() != null ? chat.getSender().getId() : chat.getStall().getId();
        String senderName = chat.getSender() != null ? chat.getSender().getClientName()
                : (chat.getStall().getUserList() != null && !chat.getStall().getUserList().isEmpty()
                        ? chat.getStall().getUserList().get(0).getName()
                        : "Stall " + chat.getStall().getLicence());

        String content = chat.isDeleted() ? "Message has been removed" : chat.getContent();
        boolean sentByStall = (chat.getSender() == null);

        return new ChatResponse(
                chat.getId(),
                chat.getConversation() != null ? chat.getConversation().getConversationId() : null,
                senderId,
                senderName,
                chat.getStall().getId(),
                chat.getCustomer().getId(),
                chat.getCustomer().getClientName(),
                content,
                chat.isReadByCustomer(),
                chat.isReadByStall(),
                chat.isDeleted(),
                sentByStall,
                chat.getCreatedAt());
    }
}
