package com.orca.com.protocol;

import java.nio.ByteBuffer;

/**
 * 评估配置请求 (Type=2)
 * 结构: [Type(2)][RequestId(8)][ResponseTerminal(2)]
 * 总长度: 2 + 8 + 2 = 12字节
 */
public class EvaluationConfigRequest extends UdpRequest {
    public static final int TYPE = 2;
    public static final int PAYLOAD_SIZE = 2; // responseTerminal (2 bytes)
    public static final int TOTAL_SIZE = 2 + 8 + PAYLOAD_SIZE; // 12字节
    
    private int responseTerminal; // uint16_t
    
    public EvaluationConfigRequest() {
    }
    
    @Override
    public int getType() {
        return TYPE;
    }

    public int getResponseTerminal() {
        return responseTerminal;
    }

    public void setResponseTerminal(int responseTerminal) {
        this.responseTerminal = responseTerminal;
    }

    /**
     * 从字节数组解码
     */
    public static EvaluationConfigRequest decode(byte[] data) {
        if (data.length < TOTAL_SIZE) {
            throw new IllegalArgumentException("EvaluationConfigRequest data too short: " + data.length);
        }
        ByteBuffer buffer = ByteOrderUtils.wrapLittleEndian(data);
        
        // 读取Type
        int type = ByteOrderUtils.readUint16(buffer);
        if (type != TYPE) {
             throw new IllegalArgumentException("Invalid type for EvaluationConfigRequest: " + type);
        }
        
        EvaluationConfigRequest request = new EvaluationConfigRequest();
        request.setRequestId(ByteOrderUtils.readUint64(buffer));
        request.setResponseTerminal(ByteOrderUtils.readUint16(buffer));
        
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
        return buffer.array();
    }
}
