package com.orca.com.protocol;

import java.nio.ByteBuffer;

/**
 * 评估配置请求 (Type=2)
 * 结构: [Type(2)][RequestId(8)]
 * 总长度: 2 + 8 = 10字节
 */
public class EvaluationConfigRequest extends UdpRequest {
    public static final int TYPE = 2;
    public static final int PAYLOAD_SIZE = 0; // 不含Type和RequestId，无额外载荷
    public static final int TOTAL_SIZE = 2 + 8 + PAYLOAD_SIZE; // 10字节
    
    public EvaluationConfigRequest() {
    }
    
    @Override
    public int getType() {
        return TYPE;
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
        return buffer.array();
    }
}
