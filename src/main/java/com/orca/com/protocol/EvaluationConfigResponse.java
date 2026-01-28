package com.orca.com.protocol;

import java.util.List;

/**
 * 评估配置响应 (Type=2)
 * 结构：
 * [Type(2)][RequestId(8)]
 * [testBackground(128字节,定长UTF-8字符串)]
 * [evaluationPurpose(128字节,定长UTF-8字符串)]
 * [evalTaskId(8字节,数字)]
 * [testPlatforms(8字节)]
 * [sonarTestLocation(2字节)]
 * [sonarTestTasks(2字节)]
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
     * 测试平台配置
     */
    private long testPlatforms;

    /**
     * 位置
     */
    private int sonarTestLocation;

    /**
     * 任务序列
     */
    private int sonarTestTasks;

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
        ByteOrderUtils.writeUint64(buffer, testPlatforms);
        ByteOrderUtils.writeUint16(buffer, sonarTestLocation);
        ByteOrderUtils.writeUint16(buffer, sonarTestTasks);
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
        response.testPlatforms = ByteOrderUtils.readUint64(buffer);
        response.sonarTestLocation = ByteOrderUtils.readUint16(buffer);
        response.sonarTestTasks = ByteOrderUtils.readUint16(buffer);
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

    public long getTestPlatforms() {
        return testPlatforms;
    }

    public void setTestPlatforms(long testPlatforms) {
        this.testPlatforms = testPlatforms;
    }

    public int getSonarTestLocation() {
        return sonarTestLocation;
    }

    public void setSonarTestLocation(int sonarTestLocation) {
        this.sonarTestLocation = sonarTestLocation;
    }

    public int getSonarTestTasks() {
        return sonarTestTasks;
    }

    public void setSonarTestTasks(int sonarTestTasks) {
        this.sonarTestTasks = sonarTestTasks;
    }

    public int getTestMethod() {
        return testMethod;
    }

    public void setTestMethod(int testMethod) {
        this.testMethod = testMethod;
    }
}
