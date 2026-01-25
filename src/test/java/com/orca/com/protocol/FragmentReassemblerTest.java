package com.orca.com.protocol;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 分片重组测试
 */
class FragmentReassemblerTest {
    
    @Test
    void testReassembleSingleFragment() {
        FragmentReassembler reassembler = new FragmentReassembler(3000);
        
        byte[] data = "Hello World".getBytes();
        FragmentHeader header = new FragmentHeader(1L, 1, 0, data.length);
        
        byte[] result = reassembler.addFragment(header, data);
        assertNotNull(result);
        assertArrayEquals(data, result);
        
        reassembler.shutdown();
    }
    
    @Test
    void testReassembleMultipleFragments() {
        FragmentReassembler reassembler = new FragmentReassembler(3000);
        
        String originalData = "This is a long message that needs to be fragmented into multiple pieces for transmission over UDP";
        byte[] data = originalData.getBytes();
        
        // 模拟分片：每个分片20字节
        int fragmentSize = 20;
        int totalPackets = (int) Math.ceil((double) data.length / fragmentSize);
        long sessionId = 12345L;
        
        byte[] result = null;
        for (int i = 0; i < totalPackets; i++) {
            int offset = i * fragmentSize;
            int currentSize = Math.min(fragmentSize, data.length - offset);
            byte[] fragment = new byte[currentSize];
            System.arraycopy(data, offset, fragment, 0, currentSize);
            
            FragmentHeader header = new FragmentHeader(sessionId, totalPackets, i, currentSize);
            result = reassembler.addFragment(header, fragment);
        }
        
        assertNotNull(result);
        assertEquals(originalData, new String(result));
        
        reassembler.shutdown();
    }
}
