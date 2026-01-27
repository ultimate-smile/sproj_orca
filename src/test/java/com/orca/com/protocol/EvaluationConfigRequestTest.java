package com.orca.com.protocol;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 评估配置请求编解码测试
 */
class EvaluationConfigRequestTest {
    
    @Test
    void testEncodeDecode() {
        EvaluationConfigRequest request = new EvaluationConfigRequest();
        request.setRequestId(1234567890123456789L);
        request.setResponseTerminal(123);
        
        // 编码
        byte[] encoded = request.encode();
        assertEquals(EvaluationConfigRequest.TOTAL_SIZE, encoded.length);
        
        // 验证 Type (2)
        assertEquals(EvaluationConfigRequest.TYPE, encoded[0]);
        assertEquals(0, encoded[1]);
        
        // 解码
        EvaluationConfigRequest decoded = EvaluationConfigRequest.decode(encoded);
        
        // 验证
        assertEquals(request.getRequestId(), decoded.getRequestId());
        assertEquals(EvaluationConfigRequest.TYPE, decoded.getType());
        assertEquals(123, decoded.getResponseTerminal());
    }
    
    @Test
    void testRequestFactoryDecode() {
        EvaluationConfigRequest request = new EvaluationConfigRequest();
        request.setRequestId(999L);
        request.setResponseTerminal(456);
        
        byte[] encoded = request.encode();
        
        // 使用工厂解码
        UdpRequest decoded = RequestFactory.decode(encoded);
        assertTrue(decoded instanceof EvaluationConfigRequest);
        EvaluationConfigRequest evalRequest = (EvaluationConfigRequest) decoded;
        assertEquals(999L, evalRequest.getRequestId());
        assertEquals(456, evalRequest.getResponseTerminal());
    }
}
