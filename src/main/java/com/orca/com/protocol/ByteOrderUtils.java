package com.orca.com.protocol;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 小端字节序工具类
 */
public class ByteOrderUtils {
    
    /**
     * 创建小端ByteBuffer
     */
    public static ByteBuffer allocateLittleEndian(int capacity) {
        return ByteBuffer.allocate(capacity).order(ByteOrder.LITTLE_ENDIAN);
    }
    
    /**
     * 包装字节数组为小端ByteBuffer
     */
    public static ByteBuffer wrapLittleEndian(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
    }
    
    /**
     * 读取uint32_t (小端)
     */
    public static long readUint32(ByteBuffer buffer) {
        return Integer.toUnsignedLong(buffer.getInt());
    }
    
    /**
     * 写入uint32_t (小端)
     */
    public static void writeUint32(ByteBuffer buffer, long value) {
        buffer.putInt((int) (value & 0xFFFFFFFFL));
    }
    
    /**
     * 读取uint16_t (小端)
     */
    public static int readUint16(ByteBuffer buffer) {
        return Short.toUnsignedInt(buffer.getShort());
    }
    
    /**
     * 写入uint16_t (小端)
     */
    public static void writeUint16(ByteBuffer buffer, int value) {
        buffer.putShort((short) (value & 0xFFFF));
    }
    
    /**
     * 读取uint8_t
     */
    public static int readUint8(ByteBuffer buffer) {
        return Byte.toUnsignedInt(buffer.get());
    }
    
    /**
     * 写入uint8_t
     */
    public static void writeUint8(ByteBuffer buffer, int value) {
        buffer.put((byte) (value & 0xFF));
    }
    
    /**
     * 读取uint64_t (小端)
     */
    public static long readUint64(ByteBuffer buffer) {
        return buffer.getLong();
    }
    
    /**
     * 写入uint64_t (小端)
     */
    public static void writeUint64(ByteBuffer buffer, long value) {
        buffer.putLong(value);
    }
    
    /**
     * 读取double (小端)
     */
    public static double readDouble(ByteBuffer buffer) {
        return buffer.getDouble();
    }
    
    /**
     * 写入double (小端)
     */
    public static void writeDouble(ByteBuffer buffer, double value) {
        buffer.putDouble(value);
    }
    
    /**
     * 读取float (小端)
     */
    public static float readFloat(ByteBuffer buffer) {
        return buffer.getFloat();
    }
    
    /**
     * 写入float (小端)
     */
    public static void writeFloat(ByteBuffer buffer, float value) {
        buffer.putFloat(value);
    }
}
