package com.orca.com.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orca.com.config.UdpProperties;
import com.orca.com.service.UdpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WebSocket端到端集成测试
 */
// @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// @TestPropertySource(properties = {
//    "orca.udp.listen-host=127.0.0.1",
//    "orca.udp.listen-port=19214",
//    "orca.udp.send-host=127.0.0.1",
//    "orca.udp.send-port=19215"
// })
class WebSocketIntegrationTest {
    
    // @LocalServerPort
    private int port;
    
    private ObjectMapper objectMapper = new ObjectMapper();
    
    // @Test
    void testWebSocketConnection() throws Exception {
        // 注意：这是一个基础测试框架
        // 完整的WebSocket测试需要实际的WebSocket客户端库
        // 这里提供测试结构，实际运行时需要配合真实的UDP服务器
        
        URI uri = URI.create("ws://localhost:" + port + "/ws");
        assertNotNull(uri);
        
        // TODO: 使用WebSocket客户端库（如Java-WebSocket）进行完整测试
        // 示例代码结构：
        // WebSocketClient client = new WebSocketClient(uri);
        // client.connect();
        // 
        // WebSocketRequest request = new WebSocketRequest();
        // request.setType(1);
        // request.setALongitude(116.397128);
        // request.setALatitude(39.909604);
        // request.setBLongitude(116.397026);
        // request.setBLatitude(39.918058);
        // request.setDataSource(1);
        // 
        // client.send(objectMapper.writeValueAsString(request));
        // 
        // String response = client.receive();
        // WebSocketResponse wsResponse = objectMapper.readValue(response, WebSocketResponse.class);
        // 
        // assertTrue(wsResponse.isSuccess());
        // assertNotNull(wsResponse.getItems());
    }
    
    @Test
    void testWebSocketRequestConversion() throws Exception {
        WebSocketRequest wsRequest = new WebSocketRequest();
        wsRequest.setType(1);
        wsRequest.setRequestId(999L);
        wsRequest.setResponseTerminal(0);
        wsRequest.setALongitude(116.397128);
        wsRequest.setALatitude(39.909604);
        wsRequest.setBLongitude(116.397026);
        wsRequest.setBLatitude(39.918058);
        wsRequest.setDataSource(1);
        
        // 验证JSON序列化
        String json = objectMapper.writeValueAsString(wsRequest);
        assertNotNull(json);
        assertTrue(json.contains("\"type\":1"));
        assertTrue(json.contains("\"aLongitude\":116.397128"));
        
        // 验证JSON反序列化
        WebSocketRequest decoded = objectMapper.readValue(json, WebSocketRequest.class);
        assertEquals(wsRequest.getType(), decoded.getType());
        assertEquals(wsRequest.getALongitude(), decoded.getALongitude());
    }
    
    @Test
    void testWebSocketResponseConversion() throws Exception {
        WebSocketResponse<WebSocketResponse.TerrainData> response = new WebSocketResponse<>();
        response.setType(1);
        response.setRequestId(999L);
        response.setSuccess(true);
        
        WebSocketResponse.TerrainData terrainData = new WebSocketResponse.TerrainData();
        terrainData.setCount(2L);
        terrainData.setItems(new java.util.ArrayList<>());
        
        WebSocketResponse.ResponseItem item1 = new WebSocketResponse.ResponseItem();
        item1.setALongitude(116.3974);
        item1.setBLongitude(116.4074);
        item1.setType(1);
        item1.setDensity(0.85f);
        item1.setField6(100);
        terrainData.getItems().add(item1);
        
        response.setData(terrainData);
        
        // 验证JSON序列化
        String json = objectMapper.writeValueAsString(response);
        assertNotNull(json);
        assertTrue(json.contains("\"success\":true"));
        assertTrue(json.contains("\"count\":2"));
        
        // 验证JSON反序列化
        WebSocketResponse decoded = objectMapper.readValue(json, WebSocketResponse.class);
        assertEquals(response.getType(), decoded.getType());
        assertEquals(response.getRequestId(), decoded.getRequestId());
        assertEquals(response.isSuccess(), decoded.isSuccess());
        
        // Data behaves as Map when deserialized without specific type info
        assertNotNull(decoded.getData());
    }

    @Test
    void testEvaluationConfigResponseConversion() throws Exception {
        WebSocketResponse<WebSocketResponse.EvaluationConfigData> response = new WebSocketResponse<>();
        response.setType(2);
        response.setRequestId(888L);
        response.setSuccess(true);
        
        WebSocketResponse.EvaluationConfigData configData = new WebSocketResponse.EvaluationConfigData();
        configData.setTestBackground("Test Background Info");
        configData.setEvaluationPurpose("Evaluation Purpose Info");
        configData.setEvalTaskId(2026001L);
        configData.setTestPlatforms(java.util.Arrays.asList(1, 2, 3));
        configData.setSonarTestLocation(java.util.Arrays.asList(10, 20));
        configData.setSonarTestTasks(java.util.Arrays.asList(100, 200));
        configData.setTestMethod(1);
        
        response.setData(configData);
        
        // Verify JSON serialization
        String json = objectMapper.writeValueAsString(response);
        assertNotNull(json);
        assertTrue(json.contains("\"type\":2"));
        assertTrue(json.contains("\"testBackground\":\"Test Background Info\""));
        assertTrue(json.contains("\"evalTaskId\":2026001"));
        assertTrue(json.contains("\"testPlatforms\":[1,2,3]"));
        
        // Verify JSON deserialization
        WebSocketResponse decoded = objectMapper.readValue(json, WebSocketResponse.class);
        assertEquals(response.getType(), decoded.getType());
        assertNotNull(decoded.getData());
    }
}
