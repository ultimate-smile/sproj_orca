package com.orca.com.protocol;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * UDP请求编解码测试
 */
class UdpRequestTest {
    
    @Test
    void testEncodeDecode() {
        // 创建测试请求
        UdpRequest request = new UdpRequest();
        request.setRequestId(1234567890123456789L);
        request.setResponseTerminal(100);
        request.setALongitude(116.3974);
        request.setALatitude(39.9093);
        request.setBLongitude(116.4074);
        request.setBLatitude(39.9193);
        request.setDataSource(1);
        
        // 编码
        byte[] encoded = request.encode();
        assertEquals(UdpRequest.REQUEST_SIZE, encoded.length);
        
        // 解码
        UdpRequest decoded = UdpRequest.decode(encoded);
        
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
    void testRealWorldExample() {
        // 真实场景：北京天安门到故宫
        UdpRequest request = new UdpRequest();
        request.setRequestId(1L);
        request.setResponseTerminal(0);
        request.setALongitude(116.397128);  // 天安门经度
        request.setALatitude(39.909604);    // 天安门纬度
        request.setBLongitude(116.397026); // 故宫经度
        request.setBLatitude(39.918058);   // 故宫纬度
        request.setDataSource(1);          // A数据源
        
        byte[] encoded = request.encode();
        UdpRequest decoded = UdpRequest.decode(encoded);
        
        assertEquals(116.397128, decoded.getALongitude(), 0.000001);
        assertEquals(39.909604, decoded.getALatitude(), 0.000001);
        assertEquals(116.397026, decoded.getBLongitude(), 0.000001);
        assertEquals(39.918058, decoded.getBLatitude(), 0.000001);
    }
}
