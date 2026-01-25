package com.orca.com.protocol;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 分片重组器
 */
public class FragmentReassembler {
    private final Map<Long, SessionFragments> sessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor;
    private final long timeoutMs;
    
    public FragmentReassembler(long timeoutMs) {
        this.timeoutMs = timeoutMs;
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "FragmentReassembler-Cleanup");
            t.setDaemon(true);
            return t;
        });
        this.cleanupExecutor.scheduleWithFixedDelay(this::cleanupExpired, timeoutMs, timeoutMs, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 添加分片
     * @return 完整数据，如果还未收齐则返回null
     */
    public byte[] addFragment(FragmentHeader header, byte[] fragmentData) {
        long sessionId = header.getSessionId();
        SessionFragments session = sessions.computeIfAbsent(sessionId, 
            k -> new SessionFragments(header.getTotalPackets()));
        
        synchronized (session) {
            session.addFragment(header.getCurrentPacket(), fragmentData);
            session.updateLastAccess();
            
            if (session.isComplete()) {
                byte[] completeData = session.assemble();
                sessions.remove(sessionId);
                return completeData;
            }
        }
        
        return null;
    }
    
    private void cleanupExpired() {
        long now = System.currentTimeMillis();
        sessions.entrySet().removeIf(entry -> {
            SessionFragments session = entry.getValue();
            synchronized (session) {
                if (now - session.getLastAccess() > timeoutMs) {
                    return true;
                }
            }
            return false;
        });
    }
    
    public void shutdown() {
        cleanupExecutor.shutdown();
    }
    
    /**
     * 会话分片集合
     */
    private static class SessionFragments {
        private final int totalPackets;
        private final Map<Integer, byte[]> fragments = new TreeMap<>();
        private long lastAccess;
        
        public SessionFragments(int totalPackets) {
            this.totalPackets = totalPackets;
            this.lastAccess = System.currentTimeMillis();
        }
        
        public void addFragment(int packetIndex, byte[] data) {
            fragments.put(packetIndex, data);
        }
        
        public boolean isComplete() {
            return fragments.size() == totalPackets;
        }
        
        public byte[] assemble() {
            int totalSize = fragments.values().stream().mapToInt(f -> f.length).sum();
            byte[] result = new byte[totalSize];
            int offset = 0;
            for (byte[] fragment : fragments.values()) {
                System.arraycopy(fragment, 0, result, offset, fragment.length);
                offset += fragment.length;
            }
            return result;
        }
        
        public long getLastAccess() {
            return lastAccess;
        }
        
        public void updateLastAccess() {
            this.lastAccess = System.currentTimeMillis();
        }
    }
}
