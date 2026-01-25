package com.orca.com.protocol;

/**
 * 响应工厂类
 */
public class ResponseFactory {
    
    /**
     * 根据类型解码响应
     */
    public static UdpResponse decode(byte[] data) {
        if (data.length < 2) {
            throw new IllegalArgumentException("Data too short to contain type");
        }
        
        // 预读Type (Little Endian)
        int type = (data[1] & 0xFF) << 8 | (data[0] & 0xFF);
        
        switch (type) {
            case TerrainResponse.TYPE:
                return TerrainResponse.decode(data);
            default:
                throw new IllegalArgumentException("Unknown response type: " + type);
        }
    }
}
