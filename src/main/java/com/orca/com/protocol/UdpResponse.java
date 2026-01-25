package com.orca.com.protocol;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * UDP响应结构（变长）
 */
public class UdpResponse {
    private long requestId;      // uint64_t (8字节)
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
        private byte[] terrainData; // 20*(N-1) 字节，多个点地形数据
        
        public ResponseItem() {
        }
        
        public static ResponseItem decode(ByteBuffer buffer, int terrainSize) {
            ResponseItem item = new ResponseItem();
            item.aLongitude = ByteOrderUtils.readDouble(buffer);
            item.bLongitude = ByteOrderUtils.readDouble(buffer);
            item.type = ByteOrderUtils.readUint32(buffer);
            item.density = ByteOrderUtils.readFloat(buffer);
            item.field6 = ByteOrderUtils.readUint16(buffer);
            if (terrainSize > 0) {
                item.terrainData = new byte[terrainSize];
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
            if (terrainData != null) {
                buffer.put(terrainData);
            }
        }
        
        public int getSize() {
            return 8 + 8 + 4 + 4 + 2 + (terrainData != null ? terrainData.length : 0);
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
    
    public UdpResponse() {
    }
    
    /**
     * 从字节数组解码（不含分片头）
     */
    public static UdpResponse decode(byte[] data) {
        ByteBuffer buffer = ByteOrderUtils.wrapLittleEndian(data);
        UdpResponse response = new UdpResponse();
        response.requestId = ByteOrderUtils.readUint64(buffer);
        response.count = ByteOrderUtils.readUint32(buffer);
        
        // 每个item的基础大小：8+8+4+4+2 = 26字节
        // 第一个item没有terrain数据，后续item有20*(N-1)字节
        int baseItemSize = 26;
        int remaining = buffer.remaining();
        
        for (int i = 0; i < response.count && remaining >= baseItemSize; i++) {
            int terrainSize = 20 * i;
            if (remaining < baseItemSize + terrainSize) {
                terrainSize = remaining - baseItemSize;
            }
            ResponseItem item = ResponseItem.decode(buffer, terrainSize);
            response.items.add(item);
            remaining = buffer.remaining();
        }
        
        return response;
    }
    
    /**
     * 编码为字节数组（不含分片头）
     */
    public byte[] encode() {
        int totalSize = 8 + 4; // requestId + count
        for (ResponseItem item : items) {
            totalSize += item.getSize();
        }
        
        ByteBuffer buffer = ByteOrderUtils.allocateLittleEndian(totalSize);
        ByteOrderUtils.writeUint64(buffer, requestId);
        ByteOrderUtils.writeUint32(buffer, count);
        for (ResponseItem item : items) {
            item.encode(buffer);
        }
        return buffer.array();
    }
    
    // Getters and Setters
    public long getRequestId() {
        return requestId;
    }
    
    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }
    
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
