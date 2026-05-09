package com.cism.backend.repository.system.chat;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cism.backend.model.system.chat.ChatModel;

@Repository
public interface ChatRepository extends JpaRepository<ChatModel, Long> {
    List<ChatModel> findByStall_IdAndCustomer_IdOrderByCreatedAtAsc(Long stallId, Long customerId);

    List<ChatModel> findByConversation_ConversationIdOrderByCreatedAtAsc(String conversationId);

    List<ChatModel> findByStall_IdOrderByCreatedAtAsc(Long stallId);

    List<ChatModel> findByCustomer_IdOrderByCreatedAtDesc(Long customerId);

    List<ChatModel> findByStall_IdOrderByCreatedAtDesc(Long stallId);
}
