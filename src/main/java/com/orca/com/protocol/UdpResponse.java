package com.orca.com.protocol;

import java.nio.ByteBuffer;

/**
 * UDP响应基类
 */
public abstract class UdpResponse {
    private long requestId;      // uint64_t (8字节)

    public UdpResponse() {
    }

    /**
     * 从字节数组解码（不含分片头）
     */
    public static UdpResponse decode(byte[] data) {
        ByteBuffer buffer = ByteOrderUtils.wrapLittleEndian(data);
        long requestId = ByteOrderUtils.readUint64(buffer);
        
        // 目前默认全部作为TerrainResponse处理，后续可根据某种标志区分
        // 如果需要区分，可以在这里添加逻辑
        return TerrainResponse.decode(buffer, requestId);
    }
    
    /**
     * 编码为字节数组（不含分片头）
     */
    public abstract byte[] encode();

    // Getters and Setters
    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }
}
