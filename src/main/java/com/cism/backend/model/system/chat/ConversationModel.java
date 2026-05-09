package com.cism.backend.model.system.chat;

import java.time.LocalDateTime;
import java.util.UUID;

import com.cism.backend.model.admin.StallModel;
import com.cism.backend.model.users.AuthModel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "conversations")
public class ConversationModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String conversationId;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private AuthModel customer;

    @ManyToOne
    @JoinColumn(name = "stall_id", nullable = false)
    private StallModel stall;

    private LocalDateTime createdAt;
    private LocalDateTime lastMessageAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (conversationId == null) {
            conversationId = UUID.randomUUID().toString();
        }
    }
}
