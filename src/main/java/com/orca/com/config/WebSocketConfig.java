package com.orca.com.config;

import com.orca.com.websocket.OrcaWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket配置
 */
@Configuration
public class WebSocketConfig {
    
    @Bean
    public HandlerMapping webSocketHandlerMapping(OrcaWebSocketHandler webSocketHandler) {
        Map<String, Object> map = new HashMap<>();
        map.put("/orca/ws", webSocketHandler);
        
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setUrlMap(map);
        mapping.setOrder(1);
        return mapping;
    }
    
    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}
