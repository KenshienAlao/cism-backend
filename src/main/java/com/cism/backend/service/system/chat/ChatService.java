package com.cism.backend.service.system.chat;

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
import com.cism.backend.exception.BadrequestException;
import com.cism.backend.model.admin.StallModel;
import com.cism.backend.model.system.chat.ChatModel;
import com.cism.backend.model.users.AuthModel;
import com.cism.backend.repository.admin.CreateStallRepository;
import com.cism.backend.repository.system.chat.ChatRepository;
import com.cism.backend.repository.users.RegisterRepository;
import com.cism.backend.util.CurrentUserLicence;

@Service
public class ChatService {

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private RegisterRepository registerRepository;

    @Autowired
    private CreateStallRepository stallRepository;

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

        ChatModel chat = ChatModel.builder()
                .sender(sender)
                .stall(stall)
                .customer(customer)
                .content(request.content())
                .isRead(false)
                .build();

        chat = chatRepository.save(chat);
        ChatResponse response = mapToResponse(chat);

        // Send to customer if they are not the sender
        if (sender == null || !sender.getId().equals(customer.getId())) {
            messagingTemplate.convertAndSendToUser(
                    customer.getEmail(),
                    "/queue/chat",
                    response);
        }

        // Broadcast to stall queue (for stall owners)
        messagingTemplate.convertAndSendToUser(
                stall.getLicence(),
                "/queue/chat",
                response);

        // If customer is the sender, they also get the response on their end via REST,
        // but we can also broadcast to them if they have multiple tabs.
        if (sender != null && sender.getId().equals(customer.getId())) {
            messagingTemplate.convertAndSendToUser(
                    sender.getEmail(),
                    "/queue/chat",
                    response);
        }

        return response;
    }

    public List<ChatResponse> getChatHistory(Long stallId, Long customerId) {
        String currentUsername = currentUserLicence.getCurrentUserEmail();

        StallModel stall = stallRepository.findById(stallId)
                .orElseThrow(() -> new BadrequestException("Stall not found", "STALL_NOT_FOUND"));

        boolean isOwner = stall.getLicence().equals(currentUsername);

        if (isOwner) {
            if (customerId != null) {
                return chatRepository.findByStall_IdAndCustomer_IdOrderByCreatedAtAsc(stallId, customerId)
                        .stream()
                        .map(this::mapToResponse)
                        .collect(Collectors.toList());
            } else {
                return chatRepository.findByStall_IdOrderByCreatedAtAsc(stallId)
                        .stream()
                        .map(this::mapToResponse)
                        .collect(Collectors.toList());
            }
        } else {
            AuthModel user = registerRepository.findByEmail(currentUsername)
                    .orElseThrow(() -> new BadrequestException("User not found", "USER_NOT_FOUND"));
            // Customer can only see their own chat history with the stall
            return chatRepository.findByStall_IdAndCustomer_IdOrderByCreatedAtAsc(stallId, user.getId())
                    .stream()
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
            // Stall owner marks messages from a specific customer as read
            unreadMessages = chatRepository.findByStall_IdAndCustomer_IdOrderByCreatedAtAsc(stallId, customerId)
                    .stream()
                    .filter(c -> !c.isRead() && c.getSender() != null) // Sender is the customer
                    .collect(Collectors.toList());
        } else {
            AuthModel user = registerRepository.findByEmail(currentUsername)
                    .orElseThrow(() -> new BadrequestException("User not found", "USER_NOT_FOUND"));
            // Customer marks messages from the stall as read
            unreadMessages = chatRepository.findByStall_IdAndCustomer_IdOrderByCreatedAtAsc(stallId, user.getId())
                    .stream()
                    .filter(c -> !c.isRead() && c.getSender() == null) // Sender is the stall
                    .collect(Collectors.toList());
        }

        if (!unreadMessages.isEmpty()) {
            unreadMessages.forEach(c -> c.setRead(true));
            chatRepository.saveAll(unreadMessages);

            // Broadcast read event via WebSocket
            Map<String, Object> readEvent = new HashMap<>();
            readEvent.put("type", "READ_RECEIPT");
            readEvent.put("stallId", stallId);
            if (isOwner) {
                readEvent.put("customerId", customerId);
                String targetUser = unreadMessages.get(0).getCustomer().getEmail();
                messagingTemplate.convertAndSendToUser(targetUser, "/queue/chat", readEvent);
            } else {
                AuthModel user = registerRepository.findByEmail(currentUsername).orElse(null);
                readEvent.put("customerId", user != null ? user.getId() : null);
                readEvent.put("readerId", user != null ? user.getId() : null);
                messagingTemplate.convertAndSendToUser(stall.getLicence(), "/queue/chat", readEvent);
            }
        }
    }

    public List<ChatThreadResponse> getChatThreads() {
        String currentUsername = currentUserLicence.getCurrentUserEmail();

        List<ChatThreadResponse> threads = new ArrayList<>();
        Map<String, ChatModel> latestMessagePerThread = new HashMap<>();

        boolean isStallOwner = stallRepository.findByLicence(currentUsername).isPresent();

        if (isStallOwner) {
            stallRepository.findByLicence(currentUsername).ifPresent(stall -> {
                List<ChatModel> stallChats = chatRepository.findByStall_IdOrderByCreatedAtDesc(stall.getId());
                for (ChatModel chat : stallChats) {
                    String threadKey = "stall_" + stall.getId() + "_customer_" + chat.getCustomer().getId();
                    latestMessagePerThread.putIfAbsent(threadKey, chat);
                }
            });
        } else {
            AuthModel user = registerRepository.findByEmail(currentUsername)
                    .orElseThrow(() -> new BadrequestException("User not found", "USER_NOT_FOUND"));
            List<ChatModel> customerChats = chatRepository.findByCustomer_IdOrderByCreatedAtDesc(user.getId());
            for (ChatModel chat : customerChats) {
                String threadKey = "stall_" + chat.getStall().getId() + "_customer_" + user.getId();
                latestMessagePerThread.putIfAbsent(threadKey, chat);
            }
        }

        for (ChatModel latest : latestMessagePerThread.values()) {
            boolean isSender = false;
            if (isStallOwner) {
                isSender = (latest.getSender() == null);
            } else {
                AuthModel user = registerRepository.findByEmail(currentUsername).orElse(null);
                isSender = (latest.getSender() != null && user != null
                        && latest.getSender().getId().equals(user.getId()));
            }

            String stallName = (latest.getStall().getUserList() != null && !latest.getStall().getUserList().isEmpty())
                    ? latest.getStall().getUserList().get(0).getName()
                    : "Stall " + latest.getStall().getLicence();

            String stallImage = (latest.getStall().getUserList() != null && !latest.getStall().getUserList().isEmpty())
                    ? latest.getStall().getUserList().get(0).getImage()
                    : null;

            threads.add(new ChatThreadResponse(
                    latest.getStall().getId(),
                    stallName,
                    stallImage,
                    latest.getCustomer().getId(),
                    latest.getCustomer().getClientName(),
                    latest.getCustomer().getAvatar(),
                    latest.getContent(),
                    latest.getCreatedAt(),
                    !latest.isRead() && !isSender));
        }

        threads.sort((a, b) -> b.lastMessageAt().compareTo(a.lastMessageAt()));
        return threads;
    }

    public ChatResponse deleteMessage(Long messageId) {
        String currentUsername = currentUserLicence.getCurrentUserEmail();
        ChatModel chat = chatRepository.findById(messageId)
                .orElseThrow(() -> new BadrequestException("Message not found", "MESSAGE_NOT_FOUND"));

        // Only the sender can delete their own message
        boolean isStallOwner = chat.getStall().getLicence().equals(currentUsername);
        boolean isSender;

        if (chat.getSender() != null) {
            isSender = chat.getSender().getEmail().equals(currentUsername);
        } else {
            // Stall-sent message (sender is null) — only stall owner can delete
            isSender = isStallOwner;
        }

        if (!isSender) {
            throw new BadrequestException("You can only remove your own messages", "NOT_MESSAGE_OWNER");
        }

        chat.setDeleted(true);
        chat = chatRepository.save(chat);

        ChatResponse response = mapToResponse(chat);

        // Broadcast deletion to both parties
        messagingTemplate.convertAndSendToUser(
                chat.getCustomer().getEmail(),
                "/queue/chat",
                Map.of("type", "MESSAGE_DELETED", "messageId", messageId,
                        "stallId", chat.getStall().getId(),
                        "customerId", chat.getCustomer().getId()));

        messagingTemplate.convertAndSendToUser(
                chat.getStall().getLicence(),
                "/queue/chat",
                Map.of("type", "MESSAGE_DELETED", "messageId", messageId,
                        "stallId", chat.getStall().getId(),
                        "customerId", chat.getCustomer().getId()));

        return response;
    }

    private ChatResponse mapToResponse(ChatModel chat) {
        Long senderId = chat.getSender() != null ? chat.getSender().getId() : chat.getStall().getId();
        String senderName = chat.getSender() != null ? chat.getSender().getClientName()
                : (chat.getStall().getUserList() != null && !chat.getStall().getUserList().isEmpty()
                        ? chat.getStall().getUserList().get(0).getName()
                        : "Stall " + chat.getStall().getLicence());

        String content = chat.isDeleted() ? "Message has been removed" : chat.getContent();

        return new ChatResponse(
                chat.getId(),
                senderId,
                senderName,
                chat.getStall().getId(),
                chat.getCustomer().getId(),
                chat.getCustomer().getClientName(),
                content,
                chat.isRead(),
                chat.isDeleted(),
                chat.getCreatedAt());
    }
}
