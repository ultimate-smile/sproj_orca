package com.orca.com.protocol;

import org.junit.jupiter.api.Test;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 完整案例：UDP报文请求 -> 响应（含分片） -> 接收重组
 */
class EndToEndFragmentSimulationTest {

    // 模拟网络传输最大单元 (MTU)，假设较小以触发分片
    private static final int MTU = 200; 
    // 分片头大小
    private static final int HEADER_SIZE = FragmentHeader.HEADER_SIZE;
    // 有效载荷大小 = MTU - 分片头
    private static final int MAX_PAYLOAD_SIZE = MTU - HEADER_SIZE;

    @Test
    void testCompleteRequestResponseCycleWithFragmentation() {
        System.out.println("=== 开始完整UDP分片通讯模拟 ===");

        // ==========================================
        // 1. 客户端：构造请求
        // ==========================================
        System.out.println("\n[客户端] 1. 构造请求...");
        TerrainRequest request = new TerrainRequest();
        request.setRequestId(1001L);
        request.setALongitude(116.40);
        request.setALatitude(39.90);
        request.setBLongitude(116.41);
        request.setBLatitude(39.91);
        request.setDataSource(1);
        
        byte[] requestData = request.encode();
        System.out.println("[客户端] 请求已编码，长度: " + requestData.length + " 字节");
        
        // 假设请求很小，不需要分片 (46字节 < 200字节)
        assertTrue(requestData.length < MTU, "演示目的：请求包应当较小不分片");

        // ==========================================
        // 2. 服务端：接收请求并处理
        // ==========================================
        System.out.println("\n[服务端] 2. 接收请求并生成大数据响应...");
        // (模拟网络传输请求...)
        UdpRequest receivedRequest = RequestFactory.decode(requestData);
        assertEquals(1001L, receivedRequest.getRequestId());

        // 生成一个较大的响应数据，强制触发分片
        UdpResponse response = new UdpResponse();
        response.setRequestId(receivedRequest.getRequestId());
        response.setCount(50); // 50个数据点，确保数据量足够大

        for (int i = 0; i < 50; i++) {
            UdpResponse.ResponseItem item = new UdpResponse.ResponseItem();
            item.setALongitude(116.40 + i * 0.0001);
            item.setBLongitude(116.41 + i * 0.0001);
            item.setType(1);
            item.setDensity(0.5f);
            item.setField6(i);
            // 每个点附带一些地形数据
            item.setTerrainData(new byte[20]); 
            response.getItems().add(item);
        }

        byte[] responseData = response.encode();
        System.out.println("[服务端] 响应数据生成完毕，总长度: " + responseData.length + " 字节");
        
        // ==========================================
        // 3. 服务端：数据分片
        // ==========================================
        System.out.println("\n[服务端] 3. 执行数据分片 (MTU=" + MTU + ", Payload=" + MAX_PAYLOAD_SIZE + ")...");
        FragmentSplitter splitter = new FragmentSplitter(MAX_PAYLOAD_SIZE);
        List<byte[]> fragments = splitter.split(responseData);
        
        System.out.println("[服务端] 数据已拆分为 " + fragments.size() + " 个分片");
        // 验证分片逻辑
        int expectedFragments = (int) Math.ceil((double) responseData.length / MAX_PAYLOAD_SIZE);
        assertEquals(expectedFragments, fragments.size());

        // ==========================================
        // 4. 网络传输 (模拟乱序)
        // ==========================================
        System.out.println("\n[网络] 4. 模拟网络传输 (打乱分片顺序)...");
        Collections.shuffle(fragments); // 模拟乱序到达

        // ==========================================
        // 5. 客户端：接收分片并重组
        // ==========================================
        System.out.println("\n[客户端] 5. 接收分片并重组...");
        FragmentReassembler reassembler = new FragmentReassembler(5000); // 5秒超时
        byte[] reassembledData = null;
        int receivedCount = 0;

        for (byte[] packet : fragments) {
            // 5.1 解析分片头
            byte[] headerBytes = new byte[HEADER_SIZE];
            System.arraycopy(packet, 0, headerBytes, 0, HEADER_SIZE);
            FragmentHeader header = FragmentHeader.decode(headerBytes);
            
            // 5.2 提取分片数据
            byte[] fragmentPayload = new byte[header.getCurrentSize()];
            System.arraycopy(packet, HEADER_SIZE, fragmentPayload, 0, header.getCurrentSize());
            
            System.out.printf("   -> 收到分片: 会话ID=%d, 序号=%d/%d, 数据大小=%d\n", 
                header.getSessionId(), header.getCurrentPacket(), header.getTotalPackets(), header.getCurrentSize());
            
            // 5.3 放入重组器
            byte[] result = reassembler.addFragment(header, fragmentPayload);
            receivedCount++;
            
            if (result != null) {
                System.out.println("   [!] 所有分片接收完毕，重组成功！");
                reassembledData = result;
            }
        }

        // ==========================================
        // 6. 客户端：解析最终数据
        // ==========================================
        System.out.println("\n[客户端] 6. 验证完整数据...");
        assertNotNull(reassembledData, "重组失败，数据为空");
        assertEquals(responseData.length, reassembledData.length, "重组后数据长度不一致");

        UdpResponse finalResponse = UdpResponse.decode(reassembledData);
        assertEquals(1001L, finalResponse.getRequestId());
        assertEquals(50, finalResponse.getCount());
        assertEquals(50, finalResponse.getItems().size());
        
        System.out.println("[客户端] 业务数据解析成功：");
        System.out.println("   - RequestID: " + finalResponse.getRequestId());
        System.out.println("   - Item Count: " + finalResponse.getCount());
        
        System.out.println("\n=== 案例演示结束 ===");
        
        reassembler.shutdown();
    }
}
