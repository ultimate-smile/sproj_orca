package com.orca.com.protocol;

import org.junit.jupiter.api.Test;
import java.util.HexFormat;
import java.util.Arrays;

/**
 * 工具类：生成用于 NetAssist 等工具测试的 Hex 字符串
 */
public class PacketGenerator {

    @Test
    void generateHexStrings() {
        System.out.println("====== NetAssist 测试报文生成 ======");
        
        generateTerrainPackets();
        generateEvaluationConfigPackets();
    }
    
    private void generateTerrainPackets() {
        // 1. 生成一个标准的 UDP Request (TerrainRequest)
        // 假设 RequestId = 1001 (0x03E9)
        TerrainRequest request = new TerrainRequest();
        request.setRequestId(1001L);
        request.setResponseTerminal(0);
        request.setALongitude(116.40);
        request.setALatitude(39.90);
        request.setBLongitude(116.41);
        request.setBLatitude(39.91);
        request.setDataSource(1);
        
        byte[] reqBytes = request.encode();
        System.out.println("\n[Terrain客户端请求报文] (RequestID = 1001)");
        System.out.println("说明：这是Java客户端发出的报文，NetAssist(19211) 应该收到此前缀开头的数据");
        printHex(reqBytes);
        
        // 2. 生成一个标准的 UDP Response (TerrainResponse)
        // RequestId 必须匹配 = 1001
        TerrainResponse response = new TerrainResponse();
        response.setRequestId(1001L);
        response.setCount(2); // 2个点
        
        // 点1
        TerrainResponse.ResponseItem item1 = new TerrainResponse.ResponseItem();
        item1.setALongitude(116.4000);
        item1.setBLongitude(116.4100);
        item1.setType(1);
        item1.setDensity(0.5f);
        item1.setField6(10);
        // 无地形数据
        response.getItems().add(item1);
        
        // 点2
        TerrainResponse.ResponseItem item2 = new TerrainResponse.ResponseItem();
        item2.setALongitude(116.4050);
        item2.setBLongitude(116.4150);
        item2.setType(2);
        item2.setDensity(0.8f);
        item2.setField6(20);
        // 带少量地形数据 (Hex: A1 B2 C3)
        item2.setTerrainData(new byte[]{(byte)0xA1, (byte)0xB2, (byte)0xC3});
        response.getItems().add(item2);
        
        byte[] respBytes = response.encode();
        
        // 3. 封装分片头 (模拟单分片情况)
        FragmentHeader header = new FragmentHeader(99999L, 1, 0, respBytes.length);
        byte[] headerBytes = header.encode();
        
        byte[] fullPacket = new byte[headerBytes.length + respBytes.length];
        System.arraycopy(headerBytes, 0, fullPacket, 0, headerBytes.length);
        System.arraycopy(respBytes, 0, fullPacket, headerBytes.length, respBytes.length);
        
        System.out.println("\n[Terrain服务端响应报文] (RequestID = 1001, 含分片头)");
        System.out.println("说明：这是你应该填入 NetAssist 发送区(发送给19210) 的内容");
        printHex(fullPacket);
    }
    
    private void generateEvaluationConfigPackets() {
        // 1. 生成 EvaluationConfigRequest
        // RequestId = 2002 (0x07D2)
        EvaluationConfigRequest request = new EvaluationConfigRequest();
        request.setRequestId(2002L);
        
        byte[] reqBytes = request.encode();
        System.out.println("\n[EvaluationConfig客户端请求报文] (RequestID = 2002)");
        printHex(reqBytes);
        
        // 2. 生成 EvaluationConfigResponse
        EvaluationConfigResponse response = new EvaluationConfigResponse();
        response.setRequestId(2002L);
        response.setTestBackground("Test Background Info");
        response.setEvaluationPurpose("Purpose of Evaluation");
        response.setEvalTaskId("TASK-2026-001");
        response.setTestPlatforms(Arrays.asList(1, 2, 5));
        response.setSonarTestLocation(Arrays.asList(100, 200));
        response.setSonarTestTasks(Arrays.asList(10, 20, 30));
        response.setTestMethod(1);
        
        byte[] respBytes = response.encode();
        
        // 3. 封装分片头
        FragmentHeader header = new FragmentHeader(88888L, 1, 0, respBytes.length);
        byte[] headerBytes = header.encode();
        
        byte[] fullPacket = new byte[headerBytes.length + respBytes.length];
        System.arraycopy(headerBytes, 0, fullPacket, 0, headerBytes.length);
        System.arraycopy(respBytes, 0, fullPacket, headerBytes.length, respBytes.length);
        
        System.out.println("\n[EvaluationConfig服务端响应报文] (RequestID = 2002, 含分片头)");
        printHex(fullPacket);
    }
    
    private void printHex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(String.format("%02X ", b));
        }
        System.out.println(sb.toString().trim());
    }
}
