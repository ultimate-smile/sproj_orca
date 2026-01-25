package com.orca.com.protocol;

/**
 * UDP请求抽象基类
 */
public abstract class UdpRequest {
    protected long requestId; // uint64_t (8字节)

    public UdpRequest() {
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
