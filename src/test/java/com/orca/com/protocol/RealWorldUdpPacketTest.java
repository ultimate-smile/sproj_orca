package com.orca.com.protocol;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 真实UDP报文测试 - 使用真实场景数据
 */
class RealWorldUdpPacketTest {
    
    /**
     * 测试真实场景：北京地区地形查询
     * 请求：从天安门(116.397128, 39.909604)到故宫(116.397026, 39.918058)
     */
    @Test
    void testRealWorldRequest() {
        TerrainRequest request = new TerrainRequest();
        request.setRequestId(20250125001L);
        request.setResponseTerminal(0);
        request.setALongitude(116.397128);  // 天安门经度
        request.setALatitude(39.909604);    // 天安门纬度
        request.setBLongitude(116.397026);  // 故宫经度
        request.setBLatitude(39.918058);    // 故宫纬度
        request.setDataSource(1);           // A数据源
        
        // 编码
        byte[] encoded = request.encode();
        assertEquals(TerrainRequest.TOTAL_SIZE, encoded.length); // 46字节 (44 + 2字节Type)
        
        // 解码验证
        TerrainRequest decoded = TerrainRequest.decode(encoded);
        assertEquals(20250125001L, decoded.getRequestId());
        assertEquals(116.397128, decoded.getALongitude(), 0.000001);
        assertEquals(39.909604, decoded.getALatitude(), 0.000001);
        assertEquals(116.397026, decoded.getBLongitude(), 0.000001);
        assertEquals(39.918058, decoded.getBLatitude(), 0.000001);
        assertEquals(1, decoded.getDataSource());
    }
    
    /**
     * 测试真实响应：包含多个地形点数据
     */
    @Test
    void testRealWorldResponse() {
        UdpResponse response = new UdpResponse();
        response.setRequestId(20250125001L);
        response.setCount(3);
        
        // 第一个点：起点附近
        UdpResponse.ResponseItem item1 = new UdpResponse.ResponseItem();
        item1.setALongitude(116.397128);
        item1.setBLongitude(116.397026);
        item1.setType(1);  // 地形类型1
        item1.setDensity(0.85f);  // 密度0.85
        item1.setField6(100);
        item1.setTerrainData(null); // 第一个点无terrain数据
        response.getItems().add(item1);
        
        // 第二个点：中间点
        UdpResponse.ResponseItem item2 = new UdpResponse.ResponseItem();
        item2.setALongitude(116.397100);
        item2.setBLongitude(116.397050);
        item2.setType(2);
        item2.setDensity(0.90f);
        item2.setField6(200);
        item2.setTerrainData(new byte[20]); // 20*(2-1) = 20字节
        response.getItems().add(item2);
        
        // 第三个点：终点附近
        UdpResponse.ResponseItem item3 = new UdpResponse.ResponseItem();
        item3.setALongitude(116.397026);
        item3.setBLongitude(116.397000);
        item3.setType(3);
        item3.setDensity(0.95f);
        item3.setField6(300);
        item3.setTerrainData(new byte[40]); // 20*(3-1) = 40字节
        response.getItems().add(item3);
        
        // 编码
        byte[] encoded = response.encode();
        
        // 解码验证
        UdpResponse decoded = UdpResponse.decode(encoded);
        assertEquals(20250125001L, decoded.getRequestId());
        assertEquals(3, decoded.getCount());
        assertEquals(3, decoded.getItems().size());
        
        // 验证每个点的数据
        UdpResponse.ResponseItem dItem1 = decoded.getItems().get(0);
        assertEquals(116.397128, dItem1.getALongitude(), 0.000001);
        assertEquals(1, dItem1.getType());
        assertEquals(0.85f, dItem1.getDensity(), 0.001f);
        assertNull(dItem1.getTerrainData());
        
        UdpResponse.ResponseItem dItem2 = decoded.getItems().get(1);
        assertEquals(116.397100, dItem2.getALongitude(), 0.000001);
        assertEquals(2, dItem2.getType());
        assertEquals(0.90f, dItem2.getDensity(), 0.001f);
        assertNotNull(dItem2.getTerrainData());
        assertEquals(20, dItem2.getTerrainData().length);
        
        UdpResponse.ResponseItem dItem3 = decoded.getItems().get(2);
        assertEquals(116.397026, dItem3.getALongitude(), 0.000001);
        assertEquals(3, dItem3.getType());
        assertEquals(0.95f, dItem3.getDensity(), 0.001f);
        assertNotNull(dItem3.getTerrainData());
        assertEquals(40, dItem3.getTerrainData().length);
    }
    
    /**
     * 测试分片场景：大数据包自动分片
     */
    @Test
    void testFragmentation() {
        // 创建一个较大的响应数据
        UdpResponse response = new UdpResponse();
        response.setRequestId(999L);
        response.setCount(10);
        
        for (int i = 0; i < 10; i++) {
            UdpResponse.ResponseItem item = new UdpResponse.ResponseItem();
            item.setALongitude(116.397128 + i * 0.001);
            item.setBLongitude(116.397026 + i * 0.001);
            item.setType(i + 1);
            item.setDensity(0.8f + i * 0.01f);
            item.setField6(100 + i * 10);
            if (i > 0) {
                item.setTerrainData(new byte[20 * i]); // 递增的terrain数据
            }
            response.getItems().add(item);
        }
        
        byte[] data = response.encode();
        
        // 使用分片器拆分
        FragmentSplitter splitter = new FragmentSplitter(100); // 每片最大100字节
        java.util.List<byte[]> fragments = splitter.split(data);
        
        assertFalse(fragments.isEmpty());
        
        // 使用重组器重组
        FragmentReassembler reassembler = new FragmentReassembler(3000);
        byte[] reassembled = null;
        
        for (byte[] fragment : fragments) {
            // 提取分片头
            byte[] headerBytes = new byte[FragmentHeader.HEADER_SIZE];
            System.arraycopy(fragment, 0, headerBytes, 0, FragmentHeader.HEADER_SIZE);
            FragmentHeader header = FragmentHeader.decode(headerBytes);
            
            // 提取数据
            byte[] fragmentData = new byte[header.getCurrentSize()];
            System.arraycopy(fragment, FragmentHeader.HEADER_SIZE, fragmentData, 0, header.getCurrentSize());
            
            reassembled = reassembler.addFragment(header, fragmentData);
        }
        
        assertNotNull(reassembled);
        assertEquals(data.length, reassembled.length);
        
        // 验证重组后的数据可以正确解码
        UdpResponse decoded = UdpResponse.decode(reassembled);
        assertEquals(response.getRequestId(), decoded.getRequestId());
        assertEquals(10, decoded.getCount());
        
        reassembler.shutdown();
    }
}
