package com.cism.backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.cism.backend.service.system.chat.ChatService;

import lombok.extern.slf4j.Slf4j;

import java.security.Principal;

@Component
@Slf4j
public class WebSocketEventListener {

    @Autowired
    private ChatService chatService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = headerAccessor.getUser();
        
        if (user != null) {
            String email = user.getName();
            log.info("User connected: {}", email);
            chatService.updatePresence(email, true);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = headerAccessor.getUser();

        if (user != null) {
            String email = user.getName();
            log.info("User disconnected: {}", email);
            chatService.updatePresence(email, false);
        }
    }
}
