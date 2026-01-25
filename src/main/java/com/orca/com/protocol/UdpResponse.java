package com.orca.com.protocol;

/**
 * UDP响应抽象基类
 */
public abstract class UdpResponse {
    protected long requestId; // uint64_t (8字节)

    public UdpResponse() {
    }

    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    /**
     * 获取消息类型
     */
    public abstract int getType();

    /**
     * 编码为字节数组
     */
    public abstract byte[] encode();
}
