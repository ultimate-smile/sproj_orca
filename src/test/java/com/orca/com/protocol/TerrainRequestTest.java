package com.orca.com.protocol;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 地形请求编解码测试
 */
class TerrainRequestTest {
    
    @Test
    void testEncodeDecode() {
        // 创建测试请求
        TerrainRequest request = new TerrainRequest();
        request.setRequestId(1234567890123456789L);
        request.setResponseTerminal(100);
        request.setALongitude(116.3974);
        request.setALatitude(39.9093);
        request.setBLongitude(116.4074);
        request.setBLatitude(39.9193);
        request.setDataSource(1);
        
        // 编码
        byte[] encoded = request.encode();
        assertEquals(TerrainRequest.TOTAL_SIZE, encoded.length);
        
        // 验证Type
        assertEquals(TerrainRequest.TYPE & 0xFF, encoded[0] & 0xFF);
        assertEquals((TerrainRequest.TYPE >> 8) & 0xFF, encoded[1] & 0xFF);
        
        // 解码
        TerrainRequest decoded = TerrainRequest.decode(encoded);
        
        // 验证
        assertEquals(request.getRequestId(), decoded.getRequestId());
        assertEquals(request.getResponseTerminal(), decoded.getResponseTerminal());
        assertEquals(request.getALongitude(), decoded.getALongitude(), 0.0001);
        assertEquals(request.getALatitude(), decoded.getALatitude(), 0.0001);
        assertEquals(request.getBLongitude(), decoded.getBLongitude(), 0.0001);
        assertEquals(request.getBLatitude(), decoded.getBLatitude(), 0.0001);
        assertEquals(request.getDataSource(), decoded.getDataSource());
    }
    
    @Test
    void testRequestFactoryDecode() {
        TerrainRequest request = new TerrainRequest();
        request.setRequestId(999L);
        request.setDataSource(2);
        
        byte[] encoded = request.encode();
        
        // 使用工厂解码
        UdpRequest decoded = RequestFactory.decode(encoded);
        assertTrue(decoded instanceof TerrainRequest);
        assertEquals(999L, decoded.getRequestId());
        assertEquals(2, ((TerrainRequest)decoded).getDataSource());
    }
}
