package com.orca.com.protocol;

import java.util.ArrayList;
import java.util.List;

/**
 * 评估配置响应 (Type=2)
 * 结构：
 * [Type(2)][RequestId(8)]
 * [testBackground(128字节,定长UTF-8字符串)]
 * [evaluationPurpose(128字节,定长UTF-8字符串)]
 * [evalTaskId(8字节,数字)]
 * [testPlatforms(8字节, 位掩码)]
 * [sonarTestLocation(2字节, ID)]
 * [sonarTestTasks(2字节, ID)]
 * [testMethod(2字节)]
 */
public class EvaluationConfigResponse extends UdpResponse {
    public static final int TYPE = 2;

    /**
     * 测试背景（20～30字），预留足够空间
     */
    private String testBackground;

    /**
     * 评估目的（20～30字）
     */
    private String evaluationPurpose;

    /**
     * 评估任务ID (改为数字类型)
     */
    private long evalTaskId;

    /**
     * 测试平台配置 (位掩码对应的列表)
     * 协议中为8字节掩码
     */
    private List<Integer> testPlatforms;

    /**
     * 位置 (协议中为2字节ID)
     * 为了前端兼容性，封装为列表
     */
    private List<Integer> sonarTestLocation;

    /**
     * 任务序列 (协议中为2字节ID)
     * 为了前端兼容性，封装为列表
     */
    private List<Integer> sonarTestTasks;

    /**
     * 评估测试方式 0、1、2（互斥选项）
     */
    private int testMethod;

    public EvaluationConfigResponse() {
    }

    @Override
    public int getType() {
        return TYPE;
    }

    @Override
    public byte[] encode() {
        // 计算总大小
        // Type(2) + RequestId(8)
        // Strings: 128 + 128 = 256
        // evalTaskId: 8
        // testPlatforms: 8
        // sonarTestLocation: 2
        // sonarTestTasks: 2
        // testMethod: 2
        
        int size = 2 + 8 + 256 + 8 + 8 + 2 + 2 + 2; 

        java.nio.ByteBuffer buffer = ByteOrderUtils.allocateLittleEndian(size);
        ByteOrderUtils.writeUint16(buffer, TYPE);
        ByteOrderUtils.writeUint64(buffer, getRequestId());

        // 写入定长字符串 (自动截断或填充)
        writeFixedString(buffer, testBackground, 128);
        writeFixedString(buffer, evaluationPurpose, 128);
        
        // 写入数值
        ByteOrderUtils.writeUint64(buffer, evalTaskId);
        
        // testPlatforms -> 8 byte bitmask
        ByteOrderUtils.writeUint64(buffer, listToBitmask(testPlatforms));
        
        // sonarTestLocation -> 2 byte ID (取列表第一个元素，或0)
        ByteOrderUtils.writeUint16(buffer, getFirstOrZero(sonarTestLocation));
        
        // sonarTestTasks -> 2 byte ID (取列表第一个元素，或0)
        ByteOrderUtils.writeUint16(buffer, getFirstOrZero(sonarTestTasks));
        
        ByteOrderUtils.writeUint16(buffer, testMethod);

        return buffer.array();
    }

    public static EvaluationConfigResponse decode(byte[] data) {
        java.nio.ByteBuffer buffer = ByteOrderUtils.wrapLittleEndian(data);
        
        // 读取Type
        int type = ByteOrderUtils.readUint16(buffer);
        if (type != TYPE) {
             throw new IllegalArgumentException("Invalid type for EvaluationConfigResponse: " + type);
        }

        EvaluationConfigResponse response = new EvaluationConfigResponse();
        response.setRequestId(ByteOrderUtils.readUint64(buffer));

        response.testBackground = readFixedString(buffer, 128);
        response.evaluationPurpose = readFixedString(buffer, 128);
        
        response.evalTaskId = ByteOrderUtils.readUint64(buffer);
        
        // testPlatforms <- 8 byte bitmask
        long platformsMask = ByteOrderUtils.readUint64(buffer);
        response.testPlatforms = bitmaskToList(platformsMask);
        
        // sonarTestLocation <- 2 byte ID
        int locationId = ByteOrderUtils.readUint16(buffer);
        response.sonarTestLocation = singleToList(locationId);
        
        // sonarTestTasks <- 2 byte ID
        int taskId = ByteOrderUtils.readUint16(buffer);
        response.sonarTestTasks = singleToList(taskId);
        
        response.testMethod = ByteOrderUtils.readUint16(buffer);

        return response;
    }

    // 辅助方法：写入定长字符串
    private static void writeFixedString(java.nio.ByteBuffer buffer, String s, int length) {
        byte[] bytes = new byte[length]; // 默认全0
        if (s != null) {
            byte[] strBytes = s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            System.arraycopy(strBytes, 0, bytes, 0, Math.min(strBytes.length, length));
        }
        buffer.put(bytes);
    }

    // 辅助方法：读取定长字符串
    private static String readFixedString(java.nio.ByteBuffer buffer, int length) {
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        // 去除尾部0
        int validLen = 0;
        while (validLen < length && bytes[validLen] != 0) {
            validLen++;
        }
        return new String(bytes, 0, validLen, java.nio.charset.StandardCharsets.UTF_8);
    }
    
    // 辅助方法：Bitmask 与 List 转换 (1-based index)
    private static long listToBitmask(List<Integer> list) {
        long mask = 0;
        if (list != null) {
            for (Integer id : list) {
                if (id != null && id >= 1 && id <= 64) {
                    mask |= (1L << (id - 1));
                }
            }
        }
        return mask;
    }
    
    private static List<Integer> bitmaskToList(long mask) {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < 64; i++) {
            if ((mask & (1L << i)) != 0) {
                list.add(i + 1);
            }
        }
        return list;
    }
    
    // 辅助方法：单值 与 List 转换
    private static int getFirstOrZero(List<Integer> list) {
        if (list != null && !list.isEmpty() && list.get(0) != null) {
            return list.get(0);
        }
        return 0;
    }
    
    private static List<Integer> singleToList(int val) {
        List<Integer> list = new ArrayList<>();
        if (val != 0) {
            list.add(val);
        }
        return list;
    }

    // Getters and Setters
    public String getTestBackground() {
        return testBackground;
    }

    public void setTestBackground(String testBackground) {
        this.testBackground = testBackground;
    }

    public String getEvaluationPurpose() {
        return evaluationPurpose;
    }

    public void setEvaluationPurpose(String evaluationPurpose) {
        this.evaluationPurpose = evaluationPurpose;
    }

    public long getEvalTaskId() {
        return evalTaskId;
    }

    public void setEvalTaskId(long evalTaskId) {
        this.evalTaskId = evalTaskId;
    }

    public List<Integer> getTestPlatforms() {
        return testPlatforms;
    }

    public void setTestPlatforms(List<Integer> testPlatforms) {
        this.testPlatforms = testPlatforms;
    }

    public List<Integer> getSonarTestLocation() {
        return sonarTestLocation;
    }

    public void setSonarTestLocation(List<Integer> sonarTestLocation) {
        this.sonarTestLocation = sonarTestLocation;
    }

    public List<Integer> getSonarTestTasks() {
        return sonarTestTasks;
    }

    public void setSonarTestTasks(List<Integer> sonarTestTasks) {
        this.sonarTestTasks = sonarTestTasks;
    }

    public int getTestMethod() {
        return testMethod;
    }

    public void setTestMethod(int testMethod) {
        this.testMethod = testMethod;
    }
}
