package com.orca.com.service;

import com.orca.com.config.UdpProperties;
import com.orca.com.protocol.FragmentHeader;
import com.orca.com.protocol.UdpRequest;
import com.orca.com.protocol.TerrainRequest;
import com.orca.com.protocol.UdpResponse;
import com.orca.com.protocol.TerrainResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UDP服务集成测试 - 模拟真实UDP通信
 */
class UdpServiceIntegrationTest {
    
    private UdpService udpService;
    private DatagramSocket mockServerSocket;
    private Thread mockServerThread;
    private volatile boolean serverRunning = false;
    private int mockServerPort = 19212; // 使用不同的端口避免冲突
    
    @BeforeEach
    void setUp() throws IOException {
        // 创建测试配置
        UdpProperties properties = new UdpProperties();
        properties.setListenHost("127.0.0.1");
        properties.setListenPort(19213); // 测试监听端口
        properties.setSendHost("127.0.0.1");
        properties.setSendPort(mockServerPort); // 模拟服务器端口
        properties.setMaxDatagramSize(1400);
        properties.setReassemblyTimeoutMs(3000);
        
        udpService = new UdpService(properties);
        udpService.start();
        
        // 启动模拟UDP服务器
        startMockServer();
    }
    
    @AfterEach
    void tearDown() {
        serverRunning = false;
        if (mockServerSocket != null && !mockServerSocket.isClosed()) {
            mockServerSocket.close();
        }
        if (mockServerThread != null) {
            try {
                mockServerThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (udpService != null) {
            udpService.stop();
        }
    }
    
    private void startMockServer() {
        serverRunning = true;
        mockServerThread = new Thread(() -> {
            try {
                mockServerSocket = new DatagramSocket(mockServerPort);
                byte[] buffer = new byte[4096];
                
                while (serverRunning) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        mockServerSocket.setSoTimeout(1000);
                        mockServerSocket.receive(packet);
                        
                        // 解析请求并生成响应
                        byte[] requestData = new byte[packet.getLength()];
                        System.arraycopy(packet.getData(), packet.getOffset(), requestData, 0, packet.getLength());
                        
                        // 这里简化处理：直接生成一个测试响应
                        UdpResponse response = createTestResponse();
                        byte[] responseData = response.encode();
                        
                        // 包装在分片头中 (模拟单分片)
                        FragmentHeader header = new FragmentHeader(response.getRequestId(), 1, 0, responseData.length);
                        byte[] headerBytes = header.encode();
                        byte[] packetData = new byte[headerBytes.length + responseData.length];
                        System.arraycopy(headerBytes, 0, packetData, 0, headerBytes.length);
                        System.arraycopy(responseData, 0, packetData, headerBytes.length, responseData.length);
                        
                        // 发送响应到监听端口
                        InetAddress clientAddress = InetAddress.getByName("127.0.0.1");
                        DatagramPacket responsePacket = new DatagramPacket(
                            packetData, packetData.length,
                            clientAddress, 19213);
                        mockServerSocket.send(responsePacket);
                    } catch (SocketTimeoutException e) {
                        // 超时继续循环
                    } catch (Exception e) {
                        if (serverRunning) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (Exception e) {
                if (serverRunning) {
                    e.printStackTrace();
                }
            }
        });
        mockServerThread.setDaemon(true);
        mockServerThread.start();
        
        // 等待服务器启动
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private UdpResponse createTestResponse() {
        TerrainResponse response = new TerrainResponse();
        response.setRequestId(12345L);
        response.setCount(2);
        
        // 第一个item
        TerrainResponse.ResponseItem item1 = new TerrainResponse.ResponseItem();
        item1.setALongitude(116.3974);
        item1.setBLongitude(116.4074);
        item1.setType(1);
        item1.setDensity(0.85f);
        item1.setField6(100);
        item1.setTerrainData(null);
        response.getItems().add(item1);
        
        // 第二个item
        TerrainResponse.ResponseItem item2 = new TerrainResponse.ResponseItem();
        item2.setALongitude(116.3984);
        item2.setBLongitude(116.4084);
        item2.setType(2);
        item2.setDensity(0.90f);
        item2.setField6(200);
        item2.setTerrainData(new byte[20]);
        response.getItems().add(item2);
        
        return response;
    }
    
    @Test
    void testSendRequestAndReceiveResponse() throws Exception {
        // 创建测试请求
        TerrainRequest request = new TerrainRequest();
        request.setRequestId(12345L);
        request.setResponseTerminal(0);
        request.setALongitude(116.397128);
        request.setALatitude(39.909604);
        request.setBLongitude(116.397026);
        request.setBLatitude(39.918058);
        request.setDataSource(1);
        
        // 发送请求并等待响应
        CompletableFuture<UdpResponse> future = udpService.sendRequest(request);
        UdpResponse response = future.get(5, TimeUnit.SECONDS);
        
        // 验证响应
        assertNotNull(response);
        assertEquals(12345L, response.getRequestId());
        
        assertTrue(response instanceof TerrainResponse);
        TerrainResponse terrainResponse = (TerrainResponse) response;
        assertEquals(2, terrainResponse.getCount());
        assertEquals(2, terrainResponse.getItems().size());
        
        // 验证第一个item
        TerrainResponse.ResponseItem item1 = terrainResponse.getItems().get(0);
        assertEquals(116.3974, item1.getALongitude(), 0.0001);
        assertEquals(116.4074, item1.getBLongitude(), 0.0001);
        assertEquals(1, item1.getType());
        assertEquals(0.85f, item1.getDensity(), 0.001f);
        assertEquals(100, item1.getField6());
        
        // 验证第二个item
        TerrainResponse.ResponseItem item2 = terrainResponse.getItems().get(1);
        assertEquals(116.3984, item2.getALongitude(), 0.0001);
        assertEquals(116.4084, item2.getBLongitude(), 0.0001);
        assertEquals(2, item2.getType());
        assertEquals(0.90f, item2.getDensity(), 0.001f);
        assertEquals(200, item2.getField6());
    }
}
