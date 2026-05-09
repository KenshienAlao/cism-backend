package com.cism.backend.repository.system.chat;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cism.backend.model.system.chat.ConversationModel;

@Repository
public interface ConversationRepository extends JpaRepository<ConversationModel, Long> {
    Optional<ConversationModel> findByCustomer_IdAndStall_Id(Long customerId, Long stallId);
    Optional<ConversationModel> findByConversationId(String conversationId);
    List<ConversationModel> findByCustomer_Id(Long customerId);
    List<ConversationModel> findByStall_Id(Long stallId);
}
