package com.orca.com.protocol;

import java.nio.ByteBuffer;

/**
 * UDP请求结构 (44字节)
 */
public class UdpRequest {
    public static final int REQUEST_SIZE = 44;
    
    private long requestId;      // uint64_t (8字节)
    private int responseTerminal; // uint16_t (2字节)
    private double aLongitude;   // double (8字节)
    private double aLatitude;     // double (8字节)
    private double bLongitude;    // double (8字节)
    private double bLatitude;     // double (8字节)
    private int dataSource;      // uint16_t (2字节) 1:A 2:B 3:C
    
    public UdpRequest() {
    }
    
    /**
     * 从字节数组解码（不含分片头）
     */
    public static UdpRequest decode(byte[] data) {
        if (data.length < REQUEST_SIZE) {
            throw new IllegalArgumentException("Request data too short: " + data.length);
        }
        ByteBuffer buffer = ByteOrderUtils.wrapLittleEndian(data);
        UdpRequest request = new UdpRequest();
        request.requestId = ByteOrderUtils.readUint64(buffer);
        request.responseTerminal = ByteOrderUtils.readUint16(buffer);
        request.aLongitude = ByteOrderUtils.readDouble(buffer);
        request.aLatitude = ByteOrderUtils.readDouble(buffer);
        request.bLongitude = ByteOrderUtils.readDouble(buffer);
        request.bLatitude = ByteOrderUtils.readDouble(buffer);
        request.dataSource = ByteOrderUtils.readUint16(buffer);
        return request;
    }
    
    /**
     * 编码为字节数组（不含分片头）
     */
    public byte[] encode() {
        ByteBuffer buffer = ByteOrderUtils.allocateLittleEndian(REQUEST_SIZE);
        ByteOrderUtils.writeUint64(buffer, requestId);
        ByteOrderUtils.writeUint16(buffer, responseTerminal);
        ByteOrderUtils.writeDouble(buffer, aLongitude);
        ByteOrderUtils.writeDouble(buffer, aLatitude);
        ByteOrderUtils.writeDouble(buffer, bLongitude);
        ByteOrderUtils.writeDouble(buffer, bLatitude);
        ByteOrderUtils.writeUint16(buffer, dataSource);
        return buffer.array();
    }
    
    // Getters and Setters
    public long getRequestId() {
        return requestId;
    }
    
    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }
    
    public int getResponseTerminal() {
        return responseTerminal;
    }
    
    public void setResponseTerminal(int responseTerminal) {
        this.responseTerminal = responseTerminal;
    }
    
    public double getALongitude() {
        return aLongitude;
    }
    
    public void setALongitude(double aLongitude) {
        this.aLongitude = aLongitude;
    }
    
    public double getALatitude() {
        return aLatitude;
    }
    
    public void setALatitude(double aLatitude) {
        this.aLatitude = aLatitude;
    }
    
    public double getBLongitude() {
        return bLongitude;
    }
    
    public void setBLongitude(double bLongitude) {
        this.bLongitude = bLongitude;
    }
    
    public double getBLatitude() {
        return bLatitude;
    }
    
    public void setBLatitude(double bLatitude) {
        this.bLatitude = bLatitude;
    }
    
    public int getDataSource() {
        return dataSource;
    }
    
    public void setDataSource(int dataSource) {
        this.dataSource = dataSource;
    }
}
