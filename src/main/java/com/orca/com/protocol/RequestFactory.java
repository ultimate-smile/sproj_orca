package com.orca.com.protocol;

/**
 * 请求工厂类
 */
public class RequestFactory {
    
    /**
     * 根据类型解码请求
     */
    public static UdpRequest decode(byte[] data) {
        if (data.length < 2) {
            throw new IllegalArgumentException("Data too short to contain type");
        }
        
        // 预读Type (不消耗buffer，因为具体类会重新读取或我们需要传递offset)
        // 这里简单起见，我们假设具体类会验证Type或者我们传递完整的byte[]
        int type = (data[1] & 0xFF) << 8 | (data[0] & 0xFF); // Little Endian
        
        switch (type) {
            case TerrainRequest.TYPE:
                return TerrainRequest.decode(data);
            default:
                throw new IllegalArgumentException("Unknown request type: " + type);
        }
    }
}
