package com.orca.com.protocol;

import java.nio.ByteBuffer;

/**
 * 分片头部结构 (15字节)
 */
public class FragmentHeader {
    public static final int HEADER_SIZE = 15;
    
    private long sessionId;      // uint32_t (4字节)
    private int totalPackets;    // uint16_t (2字节)
    private int currentPacket;   // uint16_t (2字节)
    private int currentSize;     // uint16_t (2字节)
    private int flags;           // uint8_t (1字节)
    private long checksum;       // uint32_t (4字节)
    
    public FragmentHeader() {
    }
    
    public FragmentHeader(long sessionId, int totalPackets, int currentPacket, int currentSize) {
        this.sessionId = sessionId;
        this.totalPackets = totalPackets;
        this.currentPacket = currentPacket;
        this.currentSize = currentSize;
        this.flags = 0;
        this.checksum = 0;
    }
    
    /**
     * 从字节数组解码
     */
    public static FragmentHeader decode(byte[] data) {
        ByteBuffer buffer = ByteOrderUtils.wrapLittleEndian(data);
        FragmentHeader header = new FragmentHeader();
        header.sessionId = ByteOrderUtils.readUint32(buffer);
        header.totalPackets = ByteOrderUtils.readUint16(buffer);
        header.currentPacket = ByteOrderUtils.readUint16(buffer);
        header.currentSize = ByteOrderUtils.readUint16(buffer);
        header.flags = ByteOrderUtils.readUint8(buffer);
        header.checksum = ByteOrderUtils.readUint32(buffer);
        return header;
    }
    
    /**
     * 编码为字节数组
     */
    public byte[] encode() {
        ByteBuffer buffer = ByteOrderUtils.allocateLittleEndian(HEADER_SIZE);
        ByteOrderUtils.writeUint32(buffer, sessionId);
        ByteOrderUtils.writeUint16(buffer, totalPackets);
        ByteOrderUtils.writeUint16(buffer, currentPacket);
        ByteOrderUtils.writeUint16(buffer, currentSize);
        ByteOrderUtils.writeUint8(buffer, flags);
        ByteOrderUtils.writeUint32(buffer, checksum);
        return buffer.array();
    }
    
    // Getters and Setters
    public long getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }
    
    public int getTotalPackets() {
        return totalPackets;
    }
    
    public void setTotalPackets(int totalPackets) {
        this.totalPackets = totalPackets;
    }
    
    public int getCurrentPacket() {
        return currentPacket;
    }
    
    public void setCurrentPacket(int currentPacket) {
        this.currentPacket = currentPacket;
    }
    
    public int getCurrentSize() {
        return currentSize;
    }
    
    public void setCurrentSize(int currentSize) {
        this.currentSize = currentSize;
    }
    
    public int getFlags() {
        return flags;
    }
    
    public void setFlags(int flags) {
        this.flags = flags;
    }
    
    public long getChecksum() {
        return checksum;
    }
    
    public void setChecksum(long checksum) {
        this.checksum = checksum;
    }
}
