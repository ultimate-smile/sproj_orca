package com.orca.com.protocol;

import org.junit.jupiter.api.Test;
import java.nio.ByteBuffer;
import java.util.HexFormat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 二进制报文验证测试
 * 验证报文结构是否符合预期，确保二进制兼容性
 */
class BinaryValidationTest {

    @Test
    void testTerrainRequestPacketStructure() {
        // 构造一个确定性的请求
        TerrainRequest request = new TerrainRequest();
        request.setRequestId(0x1122334455667788L);
        request.setResponseTerminal(0xAABB);
        request.setALongitude(1.0);
        request.setALatitude(2.0);
        request.setBLongitude(3.0);
        request.setBLatitude(4.0);
        request.setDataSource(0xCCDD);
        
        byte[] encoded = request.encode();
        
        // 预期结构 (46字节):
        // Type (2): 01 00 (Little Endian)
        // RequestId (8): 88 77 66 55 44 33 22 11
        // ResponseTerminal (2): BB AA
        // A Long (8): ... (double 1.0)
        // A Lat (8): ... (double 2.0)
        // B Long (8): ... (double 3.0)
        // B Lat (8): ... (double 4.0)
        // DataSource (2): DD CC
        
        assertEquals(46, encoded.length);
        
        ByteBuffer buffer = ByteBuffer.wrap(encoded);
        
        // 验证 Type
        assertEquals(0x01, buffer.get(0));
        assertEquals(0x00, buffer.get(1));
        
        // 验证 RequestId
        assertEquals((byte)0x88, buffer.get(2));
        assertEquals((byte)0x11, buffer.get(9));
        
        // 验证 DataSource (最后两字节)
        assertEquals((byte)0xDD, buffer.get(44));
        assertEquals((byte)0xCC, buffer.get(45));
    }
    
    @Test
    void testTerrainResponsePacketStructure() {
        // 构造一个确定性的响应
        TerrainResponse response = new TerrainResponse();
        response.setRequestId(0x1122334455667788L);
        response.setCount(1);
        
        TerrainResponse.ResponseItem item = new TerrainResponse.ResponseItem();
        item.setALongitude(1.0);
        item.setBLongitude(2.0);
        item.setType(0xAABBCCDDL);
        item.setDensity(1.5f);
        item.setField6(0xEEFF);
        // item.setTerrainData(null); // 无数据
        response.getItems().add(item);
        
        byte[] encoded = response.encode();
        
        // 预期结构:
        // Type (2): 01 00
        // RequestId (8): 88 77 66 55 44 33 22 11
        // Count (4): 01 00 00 00
        // Item (Variable):
        //   A Long (8)
        //   B Long (8)
        //   Type (4): DD CC BB AA
        //   Density (4): ...
        //   Field6 (2): FF EE
        //   TerrainLen (2): 00 00
        
        // Header(14) + Item(8+8+4+4+2+2) = 14 + 28 = 42字节
        assertEquals(42, encoded.length);
        
        ByteBuffer buffer = ByteBuffer.wrap(encoded);
        
        // 验证 Type
        assertEquals(0x01, buffer.get(0));
        assertEquals(0x00, buffer.get(1));
        
        // 验证 Count
        assertEquals(0x01, buffer.get(10));
        assertEquals(0x00, buffer.get(13));
        
        // 验证 Item Type (offset 14 + 8 + 8 = 30)
        assertEquals((byte)0xDD, buffer.get(30));
        assertEquals((byte)0xAA, buffer.get(33));
        
        // 验证 TerrainLen (最后两字节)
        assertEquals(0x00, buffer.get(40));
        assertEquals(0x00, buffer.get(41));
    }
    
    @Test
    void testFragmentHeaderStructure() {
        FragmentHeader header = new FragmentHeader(0x11223344L, 0x5566, 0x7788, 0x99AA);
        // sessionId: 4字节
        // totalPackets: 2字节
        // currentPacket: 2字节
        // currentSize: 2字节
        // flags: 1字节 (0)
        // checksum: 4字节 (0)
        
        byte[] encoded = header.encode();
        assertEquals(15, encoded.length);
        
        ByteBuffer buffer = ByteBuffer.wrap(encoded);
        
        // SessionId: 44 33 22 11
        assertEquals((byte)0x44, buffer.get(0));
        assertEquals((byte)0x11, buffer.get(3));
        
        // TotalPackets: 66 55
        assertEquals((byte)0x66, buffer.get(4));
        assertEquals((byte)0x55, buffer.get(5));
        
        // CurrentPacket: 88 77
        assertEquals((byte)0x88, buffer.get(6));
        assertEquals((byte)0x77, buffer.get(7));
        
        // CurrentSize: AA 99
        assertEquals((byte)0xAA, buffer.get(8));
        assertEquals((byte)0x99, buffer.get(9));
    }

    void testEvaluationConfigResponseStructure() {
        EvaluationConfigResponse response = new EvaluationConfigResponse();
        response.setRequestId(0x1122334455667788L);
        // 字符串 (128, 128)
        response.setTestBackground(null); // 全0
        response.setEvaluationPurpose(null); // 全0
        // evalTaskId (8字节)
        response.setEvalTaskId(0xAABBCCDDEEFF0011L);
        // 列表 (空)
        response.setTestPlatforms(null);
        response.setSonarTestLocation(null);
        response.setSonarTestTasks(null);
        // Method (4字节)
        response.setTestMethod(0x12345678);

        byte[] encoded = response.encode();

        // 预期结构:
        // Type (2): 02 00
        // RequestId (8): 88 77 66 55 44 33 22 11
        // TestBackground (128): 全0
        // EvaluationPurpose (128): 全0
        // EvalTaskId (8): 11 00 FF EE DD CC BB AA
        // TestPlatformsCount (2): 00 00
        // SonarTestLocationCount (2): 00 00
        // SonarTestTasksCount (2): 00 00
        // TestMethod (4): 78 56 34 12
        
        int expectedSize = 2 + 8 + 128 + 128 + 8 + 2 + 2 + 2 + 4; // 284字节
        assertEquals(expectedSize, encoded.length);

        ByteBuffer buffer = ByteBuffer.wrap(encoded);

        // 验证 Type
        assertEquals(0x02, buffer.get(0));
        assertEquals(0x00, buffer.get(1));

        // 验证 RequestId
        assertEquals((byte)0x88, buffer.get(2));

        // 跳过字符串 (2 + 8 + 128 + 128 = 266)
        // 验证 EvalTaskId (offset 266)
        assertEquals((byte)0x11, buffer.get(266));
        assertEquals((byte)0x00, buffer.get(267));
        assertEquals((byte)0xFF, buffer.get(268));
        assertEquals((byte)0xAA, buffer.get(273));

        // 验证 Counts (offset 266 + 8 = 274)
        assertEquals(0x00, buffer.get(274)); // testPlatformsCount
        assertEquals(0x00, buffer.get(276)); // sonarTestLocationCount
        assertEquals(0x00, buffer.get(278)); // sonarTestTasksCount
        
        // 验证 TestMethod (offset 280)
        assertEquals((byte)0x78, buffer.get(280));
    }
}
