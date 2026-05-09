package com.cism.backend.model.system.chat;

import java.time.LocalDateTime;

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
@Table(name = "chats")
public class ChatModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = true)
    private AuthModel sender;

    @ManyToOne
    @JoinColumn(name = "stall_id", nullable = false)
    private StallModel stall;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private AuthModel customer; // The customer who started the chat or is chatting with the stall

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private boolean isRead;

    @Builder.Default
    private boolean isDeleted = false;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
