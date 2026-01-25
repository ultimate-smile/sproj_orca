package com.orca.com.protocol;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 分片头部编解码测试
 */
class FragmentHeaderTest {
    
    @Test
    void testEncodeDecode() {
        FragmentHeader header = new FragmentHeader(12345L, 5, 2, 100);
        header.setFlags(0);
        header.setChecksum(0);
        
        byte[] encoded = header.encode();
        assertEquals(FragmentHeader.HEADER_SIZE, encoded.length);
        
        FragmentHeader decoded = FragmentHeader.decode(encoded);
        
        assertEquals(header.getSessionId(), decoded.getSessionId());
        assertEquals(header.getTotalPackets(), decoded.getTotalPackets());
        assertEquals(header.getCurrentPacket(), decoded.getCurrentPacket());
        assertEquals(header.getCurrentSize(), decoded.getCurrentSize());
        assertEquals(header.getFlags(), decoded.getFlags());
        assertEquals(header.getChecksum(), decoded.getChecksum());
    }
}
