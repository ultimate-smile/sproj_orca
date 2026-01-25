package com.orca.com.protocol;

import org.junit.jupiter.api.Test;
import java.nio.ByteBuffer;
import static org.junit.jupiter.api.Assertions.*;

/**
 * UdpRequest 基础抽象类及 RequestFactory 测试
 */
class UdpRequestTest {

    @Test
    void testUnknownType() {
        // 构造一个未知类型的请求数据
        // Type = 999 (未知)
        byte[] data = new byte[10];
        ByteBuffer buffer = ByteOrderUtils.wrapLittleEndian(data);
        ByteOrderUtils.writeUint16(buffer, 999);
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            RequestFactory.decode(data);
        });
        
        assertTrue(exception.getMessage().contains("Unknown request type"));
    }

    @Test
    void testDataTooShort() {
        // 数据太短，无法读取Type
        byte[] data = new byte[1];
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            RequestFactory.decode(data);
        });
        
        assertTrue(exception.getMessage().contains("too short"));
    }
    
    /**
     * 测试 TerrainRequest 通过 Factory 正确分发
     */
    @Test
    void testFactoryDecodesTerrainRequest() {
        TerrainRequest original = new TerrainRequest();
        original.setRequestId(12345L);
        original.setDataSource(1);
        
        byte[] encoded = original.encode();
        
        UdpRequest decoded = RequestFactory.decode(encoded);
        
        assertNotNull(decoded);
        assertTrue(decoded instanceof TerrainRequest);
        assertEquals(12345L, decoded.getRequestId());
        assertEquals(1, ((TerrainRequest)decoded).getDataSource());
    }

    /**
     * 测试基类 Getter/Setter
     */
    @Test
    void testBaseClassMethods() {
        // 使用匿名内部类测试抽象类方法
        UdpRequest request = new UdpRequest() {
            @Override
            public int getType() {
                return 0;
            }
            @Override
            public byte[] encode() {
                return new byte[0];
            }
        };
        
        request.setRequestId(8888L);
        assertEquals(8888L, request.getRequestId());
    }
}
