package com.orca.com.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orca.com.protocol.UdpRequest;
import com.orca.com.protocol.UdpResponse;
import com.orca.com.service.UdpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicLong;

/**
 * WebSocket处理器
 */
@Component
public class OrcaWebSocketHandler implements WebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(OrcaWebSocketHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final AtomicLong requestIdGenerator = new AtomicLong(1);
    
    private final UdpService udpService;
    
    public OrcaWebSocketHandler(UdpService udpService) {
        this.udpService = udpService;
    }
    
    @Override
    public Mono<Void> handle(WebSocketSession session) {
        logger.info("WebSocket connection established: {}", session.getId());
        
        return session.send(
            session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .flatMap(message -> processMessage(session, message))
                .map(session::textMessage)
        ).then()
        .doOnTerminate(() -> logger.info("WebSocket connection closed: {}", session.getId()));
    }
    
    private Mono<String> processMessage(WebSocketSession session, String message) {
        try {
            WebSocketRequest request = objectMapper.readValue(message, WebSocketRequest.class);
            
            // 验证请求类型
            if (request.getType() < 1 || request.getType() > 10) {
                return Mono.just(createErrorResponse(request.getType(), 
                    request.getRequestId() != null ? request.getRequestId() : 0,
                    "Invalid request type: " + request.getType()));
            }
            
            // 生成或使用请求ID
            long requestId = request.getRequestId() != null ? 
                request.getRequestId() : requestIdGenerator.getAndIncrement();
            
            // 转换为UDP请求
            UdpRequest udpRequest = convertToUdpRequest(request, requestId);
            
            // 发送UDP请求并等待响应
            return Mono.fromFuture(udpService.sendRequest(udpRequest))
                .map(udpResponse -> {
                    WebSocketResponse wsResponse = WebSocketResponse.fromUdpResponse(
                        udpResponse, request.getType());
                    try {
                        return objectMapper.writeValueAsString(wsResponse);
                    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .onErrorResume(e -> {
                    logger.error("Error processing UDP request", e);
                    WebSocketResponse errorResponse = WebSocketResponse.error(
                        request.getType(), requestId, e.getMessage());
                    try {
                        return Mono.just(objectMapper.writeValueAsString(errorResponse));
                    } catch (Exception ex) {
                        return Mono.error(ex);
                    }
                })
                .timeout(java.time.Duration.ofSeconds(30))
                .onErrorResume(e -> {
                    logger.error("Request timeout", e);
                    WebSocketResponse errorResponse = WebSocketResponse.error(
                        request.getType(), requestId, "Request timeout");
                    try {
                        return Mono.just(objectMapper.writeValueAsString(errorResponse));
                    } catch (Exception ex) {
                        return Mono.error(ex);
                    }
                });
        } catch (Exception e) {
            logger.error("Error parsing WebSocket message", e);
            return Mono.just(createErrorResponse(0, 0, "Invalid request format: " + e.getMessage()));
        }
    }
    
    private UdpRequest convertToUdpRequest(WebSocketRequest wsRequest, long requestId) {
        UdpRequest udpRequest = new UdpRequest();
        udpRequest.setRequestId(requestId);
        udpRequest.setResponseTerminal(wsRequest.getResponseTerminal() != null ? 
            wsRequest.getResponseTerminal() : 0);
        udpRequest.setALongitude(wsRequest.getALongitude() != null ? 
            wsRequest.getALongitude() : 0.0);
        udpRequest.setALatitude(wsRequest.getALatitude() != null ? 
            wsRequest.getALatitude() : 0.0);
        udpRequest.setBLongitude(wsRequest.getBLongitude() != null ? 
            wsRequest.getBLongitude() : 0.0);
        udpRequest.setBLatitude(wsRequest.getBLatitude() != null ? 
            wsRequest.getBLatitude() : 0.0);
        udpRequest.setDataSource(wsRequest.getDataSource() != null ? 
            wsRequest.getDataSource() : 1);
        return udpRequest;
    }
    
    private String createErrorResponse(int type, long requestId, String error) {
        try {
            WebSocketResponse response = WebSocketResponse.error(type, requestId, error);
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            return "{\"success\":false,\"error\":\"Failed to create error response\"}";
        }
    }
}
