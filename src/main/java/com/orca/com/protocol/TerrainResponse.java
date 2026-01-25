package com.orca.com.protocol;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * 地形分析响应 (Type=1)
 */
public class TerrainResponse extends UdpResponse {
    public static final int TYPE = 1;
    
    private long count;          // uint32_t (4字节)
    private List<ResponseItem> items = new ArrayList<>();
    
    /**
     * 响应项结构
     */
    public static class ResponseItem {
        private double aLongitude;  // double (8字节)
        private double bLongitude;  // double (8字节)
        private long type;          // uint32_t (4字节)
        private float density;      // float (4字节)
        private int field6;         // uint16_t (2字节)
        private byte[] terrainData; // 变长地形数据
        
        public ResponseItem() {
        }
        
        public static ResponseItem decode(ByteBuffer buffer) {
            ResponseItem item = new ResponseItem();
            item.aLongitude = ByteOrderUtils.readDouble(buffer);
            item.bLongitude = ByteOrderUtils.readDouble(buffer);
            item.type = ByteOrderUtils.readUint32(buffer);
            item.density = ByteOrderUtils.readFloat(buffer);
            item.field6 = ByteOrderUtils.readUint16(buffer);
            
            // 读取地形数据长度
            int terrainLength = ByteOrderUtils.readUint16(buffer);
            if (terrainLength > 0) {
                item.terrainData = new byte[terrainLength];
                buffer.get(item.terrainData);
            }
            return item;
        }
        
        public void encode(ByteBuffer buffer) {
            ByteOrderUtils.writeDouble(buffer, aLongitude);
            ByteOrderUtils.writeDouble(buffer, bLongitude);
            ByteOrderUtils.writeUint32(buffer, type);
            ByteOrderUtils.writeFloat(buffer, density);
            ByteOrderUtils.writeUint16(buffer, field6);
            
            int len = (terrainData != null) ? terrainData.length : 0;
            ByteOrderUtils.writeUint16(buffer, len);
            if (len > 0) {
                buffer.put(terrainData);
            }
        }
        
        public int getSize() {
            // 8+8+4+4+2 + 2(length) + data
            return 8 + 8 + 4 + 4 + 2 + 2 + (terrainData != null ? terrainData.length : 0);
        }
        
        // Getters and Setters
        public double getALongitude() {
            return aLongitude;
        }
        
        public void setALongitude(double aLongitude) {
            this.aLongitude = aLongitude;
        }
        
        public double getBLongitude() {
            return bLongitude;
        }
        
        public void setBLongitude(double bLongitude) {
            this.bLongitude = bLongitude;
        }
        
        public long getType() {
            return type;
        }
        
        public void setType(long type) {
            this.type = type;
        }
        
        public float getDensity() {
            return density;
        }
        
        public void setDensity(float density) {
            this.density = density;
        }
        
        public int getField6() {
            return field6;
        }
        
        public void setField6(int field6) {
            this.field6 = field6;
        }
        
        public byte[] getTerrainData() {
            return terrainData;
        }
        
        public void setTerrainData(byte[] terrainData) {
            this.terrainData = terrainData;
        }
    }
    
    public TerrainResponse() {
    }
    
    @Override
    public int getType() {
        return TYPE;
    }

    /**
     * 从字节数组解码
     */
    public static TerrainResponse decode(byte[] data) {
        if (data.length < 14) { // 2(Type) + 8(RequestId) + 4(Count)
            throw new IllegalArgumentException("TerrainResponse data too short: " + data.length);
        }
        ByteBuffer buffer = ByteOrderUtils.wrapLittleEndian(data);
        
        // 读取Type
        int type = ByteOrderUtils.readUint16(buffer);
        if (type != TYPE) {
             throw new IllegalArgumentException("Invalid type for TerrainResponse: " + type);
        }
        
        TerrainResponse response = new TerrainResponse();
        response.setRequestId(ByteOrderUtils.readUint64(buffer));
        response.count = ByteOrderUtils.readUint32(buffer);
        
        for (int i = 0; i < response.count && buffer.hasRemaining(); i++) {
            ResponseItem item = ResponseItem.decode(buffer);
            response.items.add(item);
        }
        
        return response;
    }
    
    /**
     * 编码为字节数组
     */
    @Override
    public byte[] encode() {
        int totalSize = 2 + 8 + 4; // Type + requestId + count
        for (ResponseItem item : items) {
            totalSize += item.getSize();
        }
        
        ByteBuffer buffer = ByteOrderUtils.allocateLittleEndian(totalSize);
        ByteOrderUtils.writeUint16(buffer, TYPE);
        ByteOrderUtils.writeUint64(buffer, getRequestId());
        ByteOrderUtils.writeUint32(buffer, count);
        for (ResponseItem item : items) {
            item.encode(buffer);
        }
        return buffer.array();
    }
    
    // Getters and Setters
    public long getCount() {
        return count;
    }
    
    public void setCount(long count) {
        this.count = count;
    }
    
    public List<ResponseItem> getItems() {
        return items;
    }
    
    public void setItems(List<ResponseItem> items) {
        this.items = items;
    }
}
