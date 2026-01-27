package com.orca.com.service;

import com.orca.com.config.UdpProperties;
import com.orca.com.protocol.EvaluationConfigRequest;
import com.orca.com.protocol.EvaluationConfigResponse;
import com.orca.com.protocol.FragmentHeader;
import com.orca.com.protocol.UdpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 模拟 EvaluationConfig 流程测试
 */
class EvaluationSimulationTest {

    private UdpService udpService;
    private DatagramSocket simulatorSocket;
    private final int clientPort = 19210;
    private final int serverPort = 19211;
    
    @BeforeEach
    void setUp() throws IOException {
        UdpProperties properties = new UdpProperties();
        properties.setListenHost("127.0.0.1");
        properties.setListenPort(clientPort);
        properties.setSendHost("127.0.0.1");
        properties.setSendPort(serverPort);
        properties.setMaxDatagramSize(1400);
        properties.setReassemblyTimeoutMs(3000);
        
        udpService = new UdpService(properties);
        udpService.start();
        
        simulatorSocket = new DatagramSocket(serverPort, InetAddress.getByName("127.0.0.1"));
        new Thread(() -> {
            try {
                byte[] buf = new byte[1024];
                while (!simulatorSocket.isClosed()) {
                    simulatorSocket.receive(new DatagramPacket(buf, buf.length));
                }
            } catch (Exception e) { }
        }).start();
    }

    @AfterEach
    void tearDown() {
        if (udpService != null) udpService.stop();
        if (simulatorSocket != null && !simulatorSocket.isClosed()) simulatorSocket.close();
    }

    @Test
    void testEvaluationConfigCycle() throws Exception {
        // 1. 发起请求
        long requestId = 2002L;
        EvaluationConfigRequest request = new EvaluationConfigRequest();
        request.setRequestId(requestId);
        
        CompletableFuture<UdpResponse> future = udpService.sendRequest(request);
        
        // 2. 动态生成响应报文 (避免硬编码 Hex 字符串出错)
        byte[] responsePacket = generateResponsePacket(requestId);
        
        DatagramPacket packet = new DatagramPacket(
            responsePacket, responsePacket.length, 
            InetAddress.getByName("127.0.0.1"), clientPort);
            
        Thread.sleep(100);
        simulatorSocket.send(packet);
        
        // 3. 验证
        UdpResponse result = future.get(5, TimeUnit.SECONDS);
        assertTrue(result instanceof EvaluationConfigResponse);
        EvaluationConfigResponse response = (EvaluationConfigResponse) result;
        
        assertEquals(requestId, response.getRequestId());
        assertEquals("Test Background Info", response.getTestBackground());
        assertEquals("Purpose of Evaluation", response.getEvaluationPurpose());
        assertEquals("TASK-2026-001", response.getEvalTaskId());
        assertEquals(3, response.getTestPlatforms().size());
        assertEquals(1, response.getTestPlatforms().get(0));
        assertEquals(1, response.getTestMethod());
    }
    
    private byte[] generateResponsePacket(long requestId) {
        EvaluationConfigResponse response = new EvaluationConfigResponse();
        response.setRequestId(requestId);
        response.setTestBackground("Test Background Info");
        response.setEvaluationPurpose("Purpose of Evaluation");
        response.setEvalTaskId("TASK-2026-001");
        response.setTestPlatforms(Arrays.asList(1, 2, 5));
        response.setSonarTestLocation(Arrays.asList(100, 200));
        response.setSonarTestTasks(Arrays.asList(10, 20, 30));
        response.setTestMethod(1);
        
        byte[] respBytes = response.encode();
        
        FragmentHeader header = new FragmentHeader(88888L, 1, 0, respBytes.length);
        byte[] headerBytes = header.encode();
        
        byte[] fullPacket = new byte[headerBytes.length + respBytes.length];
        System.arraycopy(headerBytes, 0, fullPacket, 0, headerBytes.length);
        System.arraycopy(respBytes, 0, fullPacket, headerBytes.length, respBytes.length);
        
        return fullPacket;
    }
}
