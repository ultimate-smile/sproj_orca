package com.orca.com.protocol;

import java.nio.ByteBuffer;

/**
 * 地形分析请求 (Type=1)
 * 结构: [Type(2)][RequestId(8)][ResponseTerminal(2)][A_Long(8)][A_Lat(8)][B_Long(8)][B_Lat(8)][DataSource(2)]
 * 总长度: 2 + 44 = 46字节 (相比原版增加了2字节Type)
 */
public class TerrainRequest extends UdpRequest {
    public static final int TYPE = 1;
    public static final int PAYLOAD_SIZE = 44; // 不含Type
    public static final int TOTAL_SIZE = 2 + PAYLOAD_SIZE;
    
    // requestId 继承自父类 (8字节)
    private int responseTerminal; // uint16_t (2字节)
    private double aLongitude;    // double (8字节)
    private double aLatitude;     // double (8字节)
    private double bLongitude;    // double (8字节)
    private double bLatitude;     // double (8字节)
    private int dataSource;       // uint16_t (2字节) 1:A 2:B 3:C
    
    public TerrainRequest() {
    }
    
    @Override
    public int getType() {
        return TYPE;
    }

    /**
     * 从字节数组解码
     */
    public static TerrainRequest decode(byte[] data) {
        if (data.length < TOTAL_SIZE) {
            throw new IllegalArgumentException("TerrainRequest data too short: " + data.length);
        }
        ByteBuffer buffer = ByteOrderUtils.wrapLittleEndian(data);
        
        // 读取Type
        int type = ByteOrderUtils.readUint16(buffer);
        if (type != TYPE) {
             throw new IllegalArgumentException("Invalid type for TerrainRequest: " + type);
        }
        
        TerrainRequest request = new TerrainRequest();
        request.setRequestId(ByteOrderUtils.readUint64(buffer));
        request.responseTerminal = ByteOrderUtils.readUint16(buffer);
        request.aLongitude = ByteOrderUtils.readDouble(buffer);
        request.aLatitude = ByteOrderUtils.readDouble(buffer);
        request.bLongitude = ByteOrderUtils.readDouble(buffer);
        request.bLatitude = ByteOrderUtils.readDouble(buffer);
        request.dataSource = ByteOrderUtils.readUint16(buffer);
        return request;
    }
    
    /**
     * 编码为字节数组
     */
    @Override
    public byte[] encode() {
        ByteBuffer buffer = ByteOrderUtils.allocateLittleEndian(TOTAL_SIZE);
        ByteOrderUtils.writeUint16(buffer, TYPE);
        ByteOrderUtils.writeUint64(buffer, getRequestId());
        ByteOrderUtils.writeUint16(buffer, responseTerminal);
        ByteOrderUtils.writeDouble(buffer, aLongitude);
        ByteOrderUtils.writeDouble(buffer, aLatitude);
        ByteOrderUtils.writeDouble(buffer, bLongitude);
        ByteOrderUtils.writeDouble(buffer, bLatitude);
        ByteOrderUtils.writeUint16(buffer, dataSource);
        return buffer.array();
    }
    
    // Getters and Setters
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
