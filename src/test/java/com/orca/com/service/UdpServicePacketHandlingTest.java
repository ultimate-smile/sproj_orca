package com.orca.com.service;

import com.orca.com.config.UdpProperties;
import com.orca.com.protocol.FragmentHeader;
import com.orca.com.protocol.ResponseFactory;
import com.orca.com.protocol.UdpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.InetAddress;

import static org.mockito.Mockito.*;

/**
 * UdpService 报文处理逻辑测试
 * 专门用于重现和修复 ArrayIndexOutOfBoundsException
 */
@ExtendWith(MockitoExtension.class)
class UdpServicePacketHandlingTest {

    private UdpService udpService;
    
    @Mock
    private UdpProperties properties;

    @BeforeEach
    void setUp() {
        when(properties.getReassemblyTimeoutMs()).thenReturn(1000L);
        when(properties.getMaxDatagramSize()).thenReturn(1400);
        
        udpService = new UdpService(properties);
    }

    /**
     * 重现：当报文长度不足以包含分片头指定的长度时，不应抛出异常
     */
    @Test
    void testProcessReceivedPacket_WithInsufficientDataLength() throws Exception {
        // 构造一个有问题的报文
        // 分片头指定 currentSize = 100
        // 但实际剩余数据只有 50
        
        long sessionId = 12345L;
        int currentSize = 100;
        FragmentHeader header = new FragmentHeader(sessionId, 1, 0, currentSize);
        byte[] headerBytes = header.encode(); // 15 bytes
        
        // 构造总报文：头 + 50字节数据 (比 currentSize 少)
        byte[] packetData = new byte[headerBytes.length + 50];
        System.arraycopy(headerBytes, 0, packetData, 0, headerBytes.length);
        // 填充一些垃圾数据
        for (int i = 0; i < 50; i++) {
            packetData[headerBytes.length + i] = (byte) i;
        }
        
        DatagramPacket packet = new DatagramPacket(packetData, packetData.length);
        packet.setAddress(InetAddress.getByName("127.0.0.1"));
        packet.setPort(12345);
        
        // 反射调用私有方法 processReceivedPacket
        Method processMethod = UdpService.class.getDeclaredMethod("processReceivedPacket", DatagramPacket.class);
        processMethod.setAccessible(true);
        
        // 调用方法，验证不抛出异常
        processMethod.invoke(udpService, packet);
        
        // 如果没有抛出异常，说明修复生效
    }
}
