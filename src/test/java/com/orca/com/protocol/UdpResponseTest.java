package com.orca.com.protocol;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * UDP响应编解码测试
 */
class UdpResponseTest {
    
    @Test
    void testEncodeDecodeSingleItem() {
        TerrainResponse response = new TerrainResponse();
        response.setRequestId(1234567890123456789L);
        response.setCount(1);
        
        TerrainResponse.ResponseItem item = new TerrainResponse.ResponseItem();
        item.setALongitude(116.3974);
        item.setBLongitude(116.4074);
        item.setType(1);
        item.setDensity(0.85f);
        item.setField6(100);
        item.setTerrainData(null); // 第一个item没有terrain数据
        response.getItems().add(item);
        
        // 编码
        byte[] encoded = response.encode();
        
        // 解码
        UdpResponse decodedBase = UdpResponse.decode(encoded);
        assertTrue(decodedBase instanceof TerrainResponse);
        TerrainResponse decoded = (TerrainResponse) decodedBase;
        
        // 验证
        assertEquals(response.getRequestId(), decoded.getRequestId());
        assertEquals(response.getCount(), decoded.getCount());
        assertEquals(1, decoded.getItems().size());
        
        TerrainResponse.ResponseItem decodedItem = decoded.getItems().get(0);
        assertEquals(item.getALongitude(), decodedItem.getALongitude(), 0.0001);
        assertEquals(item.getBLongitude(), decodedItem.getBLongitude(), 0.0001);
        assertEquals(item.getType(), decodedItem.getType());
        assertEquals(item.getDensity(), decodedItem.getDensity(), 0.001f);
        assertEquals(item.getField6(), decodedItem.getField6());
    }
    
    @Test
    void testEncodeDecodeMultipleItems() {
        TerrainResponse response = new TerrainResponse();
        response.setRequestId(999L);
        response.setCount(3);
        
        // 第一个item（无terrain数据）
        TerrainResponse.ResponseItem item1 = new TerrainResponse.ResponseItem();
        item1.setALongitude(116.3974);
        item1.setBLongitude(116.4074);
        item1.setType(1);
        item1.setDensity(0.85f);
        item1.setField6(100);
        item1.setTerrainData(null);
        response.getItems().add(item1);
        
        // 第二个item（有20字节terrain数据）
        TerrainResponse.ResponseItem item2 = new TerrainResponse.ResponseItem();
        item2.setALongitude(116.3984);
        item2.setBLongitude(116.4084);
        item2.setType(2);
        item2.setDensity(0.90f);
        item2.setField6(200);
        item2.setTerrainData(new byte[20]); // 20*(2-1) = 20字节
        response.getItems().add(item2);
        
        // 第三个item（有40字节terrain数据）
        TerrainResponse.ResponseItem item3 = new TerrainResponse.ResponseItem();
        item3.setALongitude(116.3994);
        item3.setBLongitude(116.4094);
        item3.setType(3);
        item3.setDensity(0.95f);
        item3.setField6(300);
        item3.setTerrainData(new byte[40]); // 20*(3-1) = 40字节
        response.getItems().add(item3);
        
        // 编码
        byte[] encoded = response.encode();
        
        // 解码
        UdpResponse decodedBase = UdpResponse.decode(encoded);
        assertTrue(decodedBase instanceof TerrainResponse);
        TerrainResponse decoded = (TerrainResponse) decodedBase;
        
        // 验证
        assertEquals(response.getRequestId(), decoded.getRequestId());
        assertEquals(3, decoded.getCount());
        assertEquals(3, decoded.getItems().size());
        
        assertEquals(item1.getALongitude(), decoded.getItems().get(0).getALongitude(), 0.0001);
        assertEquals(item2.getALongitude(), decoded.getItems().get(1).getALongitude(), 0.0001);
        assertEquals(item3.getALongitude(), decoded.getItems().get(2).getALongitude(), 0.0001);
    }
}
