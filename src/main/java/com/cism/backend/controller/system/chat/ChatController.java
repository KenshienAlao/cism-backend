package com.cism.backend.controller.system.chat;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cism.backend.dto.system.chat.ChatRequest;
import com.cism.backend.dto.system.chat.ChatResponse;
import com.cism.backend.dto.system.chat.ChatThreadResponse;
import com.cism.backend.service.system.chat.ChatService;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @PostMapping
    public ResponseEntity<ChatResponse> sendMessage(@RequestBody ChatRequest request) {
        return ResponseEntity.ok(chatService.sendMessage(request));
    }

    @GetMapping("/stall/{stallId}")
    public ResponseEntity<List<ChatResponse>> getChatHistory(
            @PathVariable Long stallId,
            @RequestParam(required = false) Long customerId) {
        return ResponseEntity.ok(chatService.getChatHistory(stallId, customerId));
    }

    @GetMapping("/threads")
    public ResponseEntity<List<ChatThreadResponse>> getChatThreads() {
        return ResponseEntity.ok(chatService.getChatThreads());
    }

    @PutMapping("/read/{stallId}")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long stallId,
            @RequestParam(required = false) Long customerId) {
        chatService.markMessagesAsRead(stallId, customerId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<ChatResponse> deleteMessage(@PathVariable Long messageId) {
        return ResponseEntity.ok(chatService.deleteMessage(messageId));
    }
}
