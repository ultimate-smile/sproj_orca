package com.orca.com.service;

import com.orca.com.config.UdpProperties;
import com.orca.com.protocol.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * UDP服务：监听19210端口，发送到19211端口
 */
@Service
public class UdpService {
    private static final Logger logger = LoggerFactory.getLogger(UdpService.class);
    
    private final UdpProperties properties;
    private DatagramSocket listenSocket;
    private DatagramSocket sendSocket;
    private InetAddress sendAddress;
    private final FragmentReassembler reassembler;
    private final FragmentSplitter splitter;
    private final ExecutorService executorService;
    private volatile boolean running = false;
    
    // 请求ID到响应回调的映射
    private final Map<Long, CompletableFuture<UdpResponse>> pendingRequests = new ConcurrentHashMap<>();
    
    public UdpService(UdpProperties properties) {
        this.properties = properties;
        this.reassembler = new FragmentReassembler(properties.getReassemblyTimeoutMs());
        this.splitter = new FragmentSplitter(properties.getMaxDatagramSize() - FragmentHeader.HEADER_SIZE);
        this.executorService = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "UdpService-Worker");
            t.setDaemon(true);
            return t;
        });
    }
    
    @PostConstruct
    public void start() throws IOException {
        // 监听socket
        listenSocket = new DatagramSocket(properties.getListenPort(), 
            InetAddress.getByName(properties.getListenHost()));
        listenSocket.setReceiveBufferSize(64 * 1024);
        
        // 发送socket
        sendSocket = new DatagramSocket();
        sendAddress = InetAddress.getByName(properties.getSendHost());
        
        running = true;
        
        // 启动接收线程
        executorService.execute(this::receiveLoop);
        
        logger.info("UDP Service started - Listening on {}:{}, Sending to {}:{}", 
            properties.getListenHost(), properties.getListenPort(),
            properties.getSendHost(), properties.getSendPort());
    }
    
    @PreDestroy
    public void stop() {
        running = false;
        if (listenSocket != null && !listenSocket.isClosed()) {
            listenSocket.close();
        }
        if (sendSocket != null && !sendSocket.isClosed()) {
            sendSocket.close();
        }
        executorService.shutdown();
        reassembler.shutdown();
        logger.info("UDP Service stopped");
    }
    
    /**
     * 发送UDP请求并等待响应
     */
    public CompletableFuture<UdpResponse> sendRequest(UdpRequest request) {
        CompletableFuture<UdpResponse> future = new CompletableFuture<>();
        pendingRequests.put(request.getRequestId(), future);
        
        try {
            byte[] requestData = request.encode();
            List<byte[]> fragments = splitter.split(requestData);
            
            for (byte[] fragment : fragments) {
                DatagramPacket packet = new DatagramPacket(
                    fragment, fragment.length, 
                    sendAddress, properties.getSendPort());
                sendSocket.send(packet);
            }
            
            logger.debug("Sent UDP request: requestId={}, fragments={}", 
                request.getRequestId(), fragments.size());
        } catch (IOException e) {
            logger.error("Failed to send UDP request", e);
            pendingRequests.remove(request.getRequestId());
            future.completeExceptionally(e);
        }
        
        return future;
    }
    
    /**
     * 接收循环
     */
    private void receiveLoop() {
        byte[] buffer = new byte[properties.getMaxDatagramSize()];
        
        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                listenSocket.receive(packet);
                
                executorService.execute(() -> processReceivedPacket(packet));
            } catch (SocketException e) {
                if (running) {
                    logger.error("Socket error in receive loop", e);
                }
                break;
            } catch (IOException e) {
                logger.error("IO error in receive loop", e);
            }
        }
    }
    
    /**
     * 处理接收到的数据包
     */
    private void processReceivedPacket(DatagramPacket packet) {
        try {
            // [1] 申请新内存：创建一个与实际接收数据长度一致的字节数组
            byte[] data = new byte[packet.getLength()];
            // [2] 数据快照：将 DatagramPacket 中的数据（可能来自复用的缓存池）复制到新数组 data 中
            // 这一步非常关键，因为 UDP 接收循环通常复用同一个 byte[] buffer，如果不复制，
            // 后续数据到达会覆盖当前处理的数据（线程安全问题）
            System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());
            
            logger.info("Received data (first 16 bytes): {}", java.util.HexFormat.of().formatHex(data, 0, Math.min(data.length, 16)));
            
            // [3] 基础校验：如果总长度连分片头（15字节）都不到，直接丢弃
            if (data.length < FragmentHeader.HEADER_SIZE) {
                logger.warn("Received packet too short: {}", data.length);
                return;
            }
            // 解析分片头
            // [4] 提取头部（低效点）：又申请了 15 字节的新数组，再次发生内存复制
            byte[] headerBytes = new byte[FragmentHeader.HEADER_SIZE];
            System.arraycopy(data, 0, headerBytes, 0, FragmentHeader.HEADER_SIZE);
            // [5] 解码头部：反序列化为 Java 对象
            FragmentHeader header = FragmentHeader.decode(headerBytes);

            // [6] 安全校验：确保报文剩余长度足够 header 声明的 payload 长度（防止越界异常）
            if (FragmentHeader.HEADER_SIZE + header.getCurrentSize() > data.length) {
                logger.error("Invalid packet size: header says {} bytes of data, but packet only has {} bytes remaining",
                    header.getCurrentSize(), data.length - FragmentHeader.HEADER_SIZE);
                return;
            }
            // [7] 提取载荷（低效点）：又申请了 payload 长度的新数组，第三次发生内存复制
            byte[] fragmentData = new byte[header.getCurrentSize()];
            System.arraycopy(data, FragmentHeader.HEADER_SIZE, fragmentData, 0, header.getCurrentSize());
            
            //[8] 重组逻辑：将头和数据交给重组器
            byte[] completeData = reassembler.addFragment(header, fragmentData);
            
            if (completeData != null) {
                // 完整数据已收齐，解析响应
                processCompleteResponse(completeData);
            }
        } catch (Exception e) {
            logger.error("Error processing received packet", e);
        }
    }
    
    /**
     * 处理完整的响应数据
     */
    private void processCompleteResponse(byte[] data) {
        try {
            UdpResponse response = ResponseFactory.decode(data);
            CompletableFuture<UdpResponse> future = pendingRequests.remove(response.getRequestId());
            
            if (future != null) {
                future.complete(response);
                if (response instanceof TerrainResponse) {
                    logger.debug("Received UDP response: requestId={}, count={}", 
                        response.getRequestId(), ((TerrainResponse)response).getCount());
                } else {
                    logger.debug("Received UDP response: requestId={}, type={}", 
                        response.getRequestId(), response.getType());
                }
            } else {
                logger.warn("No pending request found for response: requestId={}", 
                    response.getRequestId());
            }
        } catch (Exception e) {
            logger.error("Error decoding response", e);
        }
    }
}
