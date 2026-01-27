package com.orca.com;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orca.com.config.UdpProperties;
import com.orca.com.protocol.*;
import com.orca.com.service.UdpService;
import com.orca.com.websocket.WebSocketRequest;
import com.orca.com.websocket.WebSocketResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 端到端测试：WebSocket请求 -> UDP请求 -> UDP响应 -> WebSocket响应
 */
class EndToEndTest {
    
    private UdpService udpService;
    private DatagramSocket mockUdpServer;
    private Thread serverThread;
    private volatile boolean serverRunning = false;
    private int serverPort = 19216;
    private int clientPort = 19217;
    private ObjectMapper objectMapper = new ObjectMapper();
    
    @BeforeEach
    void setUp() throws IOException {
        // 配置UDP服务
        UdpProperties properties = new UdpProperties();
        properties.setListenHost("127.0.0.1");
        properties.setListenPort(clientPort);
        properties.setSendHost("127.0.0.1");
        properties.setSendPort(serverPort);
        properties.setMaxDatagramSize(1400);
        properties.setReassemblyTimeoutMs(5000);
        
        udpService = new UdpService(properties);
        udpService.start();
        
        // 启动模拟UDP服务器
        startMockUdpServer();
    }
    
    @AfterEach
    void tearDown() {
        serverRunning = false;
        if (mockUdpServer != null && !mockUdpServer.isClosed()) {
            mockUdpServer.close();
        }
        if (serverThread != null) {
            try {
                serverThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (udpService != null) {
            udpService.stop();
        }
    }
    
    private void startMockUdpServer() {
        serverRunning = true;
        serverThread = new Thread(() -> {
            try {
                mockUdpServer = new DatagramSocket(serverPort);
                byte[] buffer = new byte[4096];
                
                while (serverRunning) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        mockUdpServer.setSoTimeout(1000);
                        mockUdpServer.receive(packet);
                        
                        // 解析UDP请求
                        byte[] receivedData = new byte[packet.getLength()];
                        System.arraycopy(packet.getData(), packet.getOffset(), receivedData, 0, packet.getLength());
                        
                        // 检查是否有分片头
                        if (receivedData.length >= FragmentHeader.HEADER_SIZE) {
                            byte[] headerBytes = new byte[FragmentHeader.HEADER_SIZE];
                            System.arraycopy(receivedData, 0, headerBytes, 0, FragmentHeader.HEADER_SIZE);
                            FragmentHeader header = FragmentHeader.decode(headerBytes);
                            
                            // 如果是单分片或最后一个分片，处理请求
                            if (header.getTotalPackets() == 1 || 
                                header.getCurrentPacket() == header.getTotalPackets() - 1) {
                                
                                // 提取请求数据（简化：假设单分片）
                                byte[] requestData = new byte[header.getCurrentSize()];
                                System.arraycopy(receivedData, FragmentHeader.HEADER_SIZE, 
                                    requestData, 0, header.getCurrentSize());
                                
                                // 解析请求
                                UdpRequest request = RequestFactory.decode(requestData);
                                
                                // 生成响应
                                UdpResponse response = createRealisticResponse(request);
                                
                                // 发送响应（可能需要分片）
                                sendResponse(response, packet.getAddress(), clientPort);
                            }
                        }
                    } catch (SocketTimeoutException e) {
                        // 超时继续
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
        serverThread.setDaemon(true);
        serverThread.start();
        
        // 等待服务器启动
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private UdpResponse createRealisticResponse(UdpRequest request) {
        // 根据请求生成真实的响应数据
        TerrainResponse response = new TerrainResponse();
        response.setRequestId(request.getRequestId());
        response.setCount(3);

        double aLongitude = 0;
        double bLongitude = 0;
        
        if (request instanceof TerrainRequest) {
            TerrainRequest terrainRequest = (TerrainRequest) request;
            aLongitude = terrainRequest.getALongitude();
            bLongitude = terrainRequest.getBLongitude();
        }
        
        // 生成3个地形点数据
        for (int i = 0; i < 3; i++) {
            TerrainResponse.ResponseItem item = new TerrainResponse.ResponseItem();
            // 在A和B之间插值
            double ratio = (i + 1) / 4.0;
            item.setALongitude(aLongitude + 
                (bLongitude - aLongitude) * ratio);
            item.setBLongitude(bLongitude - 
                (bLongitude - aLongitude) * ratio);
            item.setType(i + 1);
            item.setDensity(0.8f + i * 0.05f);
            item.setField6(100 + i * 50);
            
            if (i > 0) {
                item.setTerrainData(new byte[20 * i]);
            }
            
            response.getItems().add(item);
        }
        
        return response;
    }
    
    private void sendResponse(UdpResponse response, InetAddress clientAddress, int clientPort) 
            throws IOException {
        byte[] responseData = response.encode();
        
        // 如果需要分片
        FragmentSplitter splitter = new FragmentSplitter(1300); // 留出分片头空间
        java.util.List<byte[]> fragments = splitter.split(responseData);
        
        for (byte[] fragment : fragments) {
            DatagramPacket responsePacket = new DatagramPacket(
                fragment, fragment.length,
                clientAddress, clientPort);
            mockUdpServer.send(responsePacket);
        }
    }
    
    /**
     * 测试完整的请求-响应流程
     */
    @Test
    void testCompleteRequestResponseFlow() throws Exception {
        // 1. 创建WebSocket请求（模拟）
        WebSocketRequest wsRequest = new WebSocketRequest();
        wsRequest.setType(1);
        wsRequest.setRequestId(12345L);
        wsRequest.setResponseTerminal(0);
        wsRequest.setALongitude(116.397128);  // 天安门
        wsRequest.setALatitude(39.909604);
        wsRequest.setBLongitude(116.397026);  // 故宫
        wsRequest.setBLatitude(39.918058);
        wsRequest.setDataSource(1);
        
        // 2. 转换为UDP请求
        TerrainRequest udpRequest = new TerrainRequest();
        udpRequest.setRequestId(wsRequest.getRequestId());
        udpRequest.setResponseTerminal(wsRequest.getResponseTerminal());
        udpRequest.setALongitude(wsRequest.getALongitude());
        udpRequest.setALatitude(wsRequest.getALatitude());
        udpRequest.setBLongitude(wsRequest.getBLongitude());
        udpRequest.setBLatitude(wsRequest.getBLatitude());
        udpRequest.setDataSource(wsRequest.getDataSource());
        
        // 3. 发送UDP请求并接收响应
        CompletableFuture<UdpResponse> future = udpService.sendRequest(udpRequest);
        UdpResponse udpResponse = future.get(10, TimeUnit.SECONDS);
        
        // 4. 验证UDP响应
        assertNotNull(udpResponse);
        assertEquals(12345L, udpResponse.getRequestId());
        
        assertTrue(udpResponse instanceof TerrainResponse);
        TerrainResponse terrainResponse = (TerrainResponse) udpResponse;
        
        assertEquals(3, terrainResponse.getCount());
        assertEquals(3, terrainResponse.getItems().size());
        
        // 验证响应数据的语义正确性
        TerrainResponse.ResponseItem firstItem = terrainResponse.getItems().get(0);
        assertTrue(firstItem.getALongitude() >= Math.min(wsRequest.getALongitude(), wsRequest.getBLongitude()));
        assertTrue(firstItem.getALongitude() <= Math.max(wsRequest.getALongitude(), wsRequest.getBLongitude()));
        
        // 5. 转换为WebSocket响应
        WebSocketResponse<WebSocketResponse.TerrainData> wsResponse = WebSocketResponse.fromUdpResponse(terrainResponse, wsRequest.getType());
        
        // 6. 验证WebSocket响应
        assertEquals(wsRequest.getType(), wsResponse.getType());
        assertEquals(wsRequest.getRequestId(), wsResponse.getRequestId());
        assertTrue(wsResponse.isSuccess());
        WebSocketResponse.TerrainData terrainData = wsResponse.getData();
        assertEquals(3, terrainData.getCount());
        assertEquals(3, terrainData.getItems().size());
        
        // 7. 验证JSON序列化
        String json = objectMapper.writeValueAsString(wsResponse);
        assertNotNull(json);
        assertTrue(json.contains("\"success\":true"));
        assertTrue(json.contains("\"count\":3"));
        
        // 8. 验证数据语义
        WebSocketResponse.ResponseItem wsItem1 = terrainData.getItems().get(0);
        assertEquals(firstItem.getALongitude(), wsItem1.getALongitude(), 0.0001);
        assertEquals(firstItem.getBLongitude(), wsItem1.getBLongitude(), 0.0001);
        assertEquals(firstItem.getType(), wsItem1.getType());
        assertEquals(firstItem.getDensity(), wsItem1.getDensity(), 0.001f);
        assertEquals(firstItem.getField6(), wsItem1.getField6());
    }
    
    /**
     * 测试多个连续请求
     */
    @Test
    void testMultipleRequests() throws Exception {
        for (int i = 1; i <= 5; i++) {
            TerrainRequest request = new TerrainRequest();
            request.setRequestId(10000L + i);
            request.setResponseTerminal(0);
            request.setALongitude(116.397128 + i * 0.001);
            request.setALatitude(39.909604 + i * 0.001);
            request.setBLongitude(116.397026 + i * 0.001);
            request.setBLatitude(39.918058 + i * 0.001);
            request.setDataSource(1);
            
            CompletableFuture<UdpResponse> future = udpService.sendRequest(request);
            UdpResponse response = future.get(5, TimeUnit.SECONDS);
            
            assertNotNull(response);
            assertEquals(10000L + i, response.getRequestId());
            assertTrue(response instanceof TerrainResponse);
            assertTrue(((TerrainResponse)response).getCount() > 0);
        }
    }
}
