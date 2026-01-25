package com.orca.com.protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 分片拆分器
 */
public class FragmentSplitter {
    private static final Random RANDOM = new Random();
    private final int maxFragmentSize;
    
    public FragmentSplitter(int maxFragmentSize) {
        this.maxFragmentSize = maxFragmentSize;
    }
    
    /**
     * 将数据拆分为多个分片
     * @param data 要拆分的数据
     * @return 分片列表（包含分片头）
     */
    public List<byte[]> split(byte[] data) {
        List<byte[]> fragments = new ArrayList<>();
        
        if (data.length <= maxFragmentSize) {
            // 单个分片
            long sessionId = generateSessionId();
            FragmentHeader header = new FragmentHeader(sessionId, 1, 0, data.length);
            byte[] headerBytes = header.encode();
            byte[] fragment = new byte[headerBytes.length + data.length];
            System.arraycopy(headerBytes, 0, fragment, 0, headerBytes.length);
            System.arraycopy(data, 0, fragment, headerBytes.length, data.length);
            fragments.add(fragment);
        } else {
            // 多个分片
            long sessionId = generateSessionId();
            int totalPackets = (int) Math.ceil((double) data.length / maxFragmentSize);
            int offset = 0;
            
            for (int i = 0; i < totalPackets; i++) {
                int currentSize = Math.min(maxFragmentSize, data.length - offset);
                FragmentHeader header = new FragmentHeader(sessionId, totalPackets, i, currentSize);
                byte[] headerBytes = header.encode();
                byte[] fragment = new byte[headerBytes.length + currentSize];
                System.arraycopy(headerBytes, 0, fragment, 0, headerBytes.length);
                System.arraycopy(data, offset, fragment, headerBytes.length, currentSize);
                fragments.add(fragment);
                offset += currentSize;
            }
        }
        
        return fragments;
    }
    
    private long generateSessionId() {
        return RANDOM.nextLong() & 0xFFFFFFFFL; // uint32_t范围
    }
}
