package com.orca.com.service;

import com.orca.com.config.UdpProperties;
import com.orca.com.protocol.ResponseFactory;
import com.orca.com.protocol.TerrainRequest;
import com.orca.com.protocol.TerrainResponse;
import com.orca.com.protocol.UdpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.*;
import java.util.HexFormat;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 模拟 NetAssist 真实报文测试
 * 使用 PacketGenerator 生成的已知 Hex 字符串进行测试，验证服务端接收解析逻辑
 */
class SimulatedNetAssistTest {

    private UdpService udpService;
    private DatagramSocket simulatorSocket; // 模拟 NetAssist 的 Socket
    private final int clientPort = 19210; // Java客户端监听端口
    private final int serverPort = 19211; // 模拟服务端端口 (NetAssist)
    
    // PacketGenerator 生成的已知响应报文 (含分片头)
    // RequestID = 1001 (0x03E9)
    private static final String HEX_RESPONSE_PACKET = 
        "9F 86 01 00 01 00 00 00 49 00 00 00 00 00 00 01 00 E9 03 00 00 00 00 00 00 02 00 00 00 9A 99 99 99 99 19 5D 40 0A D7 A3 70 3D 1A 5D 40 01 00 00 00 00 00 00 3F 0A 00 00 00 52 B8 1E 85 EB 19 5D 40 C3 F5 28 5C 8F 1A 5D 40 02 00 00 00 CD CC 4C 3F 14 00 03 00 A1 B2 C3";

    @BeforeEach
    void setUp() throws IOException {
        // 1. 启动 UDP 服务 (客户端)
        UdpProperties properties = new UdpProperties();
        properties.setListenHost("127.0.0.1");
        properties.setListenPort(clientPort);
        properties.setSendHost("127.0.0.1");
        properties.setSendPort(serverPort);
        properties.setMaxDatagramSize(1400);
        properties.setReassemblyTimeoutMs(3000);
        
        udpService = new UdpService(properties);
        udpService.start();
        
        // 2. 启动模拟 Socket (服务端/NetAssist)
        simulatorSocket = new DatagramSocket(serverPort, InetAddress.getByName("127.0.0.1"));
        
        // 启动一个线程来接收并丢弃客户端发的请求（避免缓冲区满，虽然本测试不重点关注请求）
        new Thread(() -> {
            try {
                byte[] buf = new byte[1024];
                while (!simulatorSocket.isClosed()) {
                    DatagramPacket p = new DatagramPacket(buf, buf.length);
                    simulatorSocket.receive(p);
                }
            } catch (Exception e) {
                // ignore
            }
        }).start();
    }

    @AfterEach
    void tearDown() {
        if (udpService != null) {
            udpService.stop();
        }
        if (simulatorSocket != null && !simulatorSocket.isClosed()) {
            simulatorSocket.close();
        }
    }

    @Test
    void testReceiveSpecificHexPacket() throws Exception {
        // 1. 客户端发起请求 (RequestID = 1001)
        // 必须发起请求，因为 UdpService 会在 pendingRequests 中注册回调
        TerrainRequest request = new TerrainRequest();
        request.setRequestId(1001L);
        // 其他字段不重要，关键是 RequestID
        request.setDataSource(1); 
        
        CompletableFuture<UdpResponse> future = udpService.sendRequest(request);
        
        // 2. 模拟 NetAssist 发送已知的 Hex 报文
        // 将 Hex 字符串转换为 byte[]
        byte[] responseData = hexStringToByteArray(HEX_RESPONSE_PACKET.replace(" ", ""));
        
        DatagramPacket packet = new DatagramPacket(
            responseData, responseData.length, 
            InetAddress.getByName("127.0.0.1"), clientPort);
            
        // 稍微延迟一点发送，确保客户端已注册好回调
        Thread.sleep(100);
        simulatorSocket.send(packet);
        System.out.println("[模拟服务端] 已发送 Hex 报文 (" + responseData.length + " bytes)");
        
        // 3. 等待并验证结果
        UdpResponse result = future.get(5, TimeUnit.SECONDS);
        
        assertNotNull(result, "未收到响应");
        assertTrue(result instanceof TerrainResponse, "响应类型错误");
        
        TerrainResponse response = (TerrainResponse) result;
        assertEquals(1001L, response.getRequestId(), "RequestID 不匹配");
        assertEquals(2, response.getCount(), "数据点数量不匹配");
        assertEquals(2, response.getItems().size(), "解析出的列表大小不匹配");
        
        // 验证第一个点 (根据 PacketGenerator 中的定义)
        // item1.setALongitude(116.4000);
        // item1.setType(1);
        TerrainResponse.ResponseItem item1 = response.getItems().get(0);
        assertEquals(116.4000, item1.getALongitude(), 0.0001);
        assertEquals(1, item1.getType());
        assertNull(item1.getTerrainData(), "第一个点应该没有地形数据");
        
        // 验证第二个点
        // item2.setType(2);
        // item2.setTerrainData(new byte[]{(byte)0xA1, (byte)0xB2, (byte)0xC3});
        TerrainResponse.ResponseItem item2 = response.getItems().get(1);
        assertEquals(2, item2.getType());
        assertNotNull(item2.getTerrainData());
        assertArrayEquals(new byte[]{(byte)0xA1, (byte)0xB2, (byte)0xC3}, item2.getTerrainData());
        
        System.out.println("[测试通过] 成功接收并解析 Hex 报文");
    }
    
    // 辅助方法：Hex转Byte数组
    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}
