package com.orca.com.protocol;

import java.util.List;

/**
 * 评估配置响应 (Type=2)
 * 结构：
 * [Type(2)][RequestId(8)]
 * [testBackground(128字节,定长UTF-8字符串)]
 * [evaluationPurpose(128字节,定长UTF-8字符串)]
 * [evalTaskId(64字节,定长UTF-8字符串)]
 * [testPlatformsCount(2)][testPlatforms(4 * N)]
 * [sonarTestLocationCount(2)][sonarTestLocation(4 * N)]
 * [sonarTestTasksCount(2)][sonarTestTasks(4 * N)]
 * [testMethod(4)]
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
     * 评估任务ID
     */
    private String evalTaskId;

    /**
     * 测试平台配置列表
     * 格式：1.1**, 2.2**, 5.5**, 7.数字**
     */
    private List<Integer> testPlatforms;

    /**
     * 位置（多个）
     */
    private List<Integer> sonarTestLocation;

    /**
     * 任务序列（多个）
     */
    private List<Integer> sonarTestTasks;

    /**
     * 评估测试方式 0、1、2（互斥选项）
     */
    private Integer testMethod;

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
        // Strings: 128 + 128 + 64 = 320
        // Lists: 2 + (4*N) for each list
        // Integer: 4
        
        int size = 2 + 8 + 320 + 4; // 基础大小
        size += 2 + (testPlatforms != null ? testPlatforms.size() * 4 : 0);
        size += 2 + (sonarTestLocation != null ? sonarTestLocation.size() * 4 : 0);
        size += 2 + (sonarTestTasks != null ? sonarTestTasks.size() * 4 : 0);

        java.nio.ByteBuffer buffer = ByteOrderUtils.allocateLittleEndian(size);
        ByteOrderUtils.writeUint16(buffer, TYPE);
        ByteOrderUtils.writeUint64(buffer, getRequestId());

        // 写入定长字符串 (自动截断或填充)
        writeFixedString(buffer, testBackground, 128);
        writeFixedString(buffer, evaluationPurpose, 128);
        writeFixedString(buffer, evalTaskId, 64);

        // 写入列表
        writeIntegerList(buffer, testPlatforms);
        writeIntegerList(buffer, sonarTestLocation);
        writeIntegerList(buffer, sonarTestTasks);

        // 写入 testMethod
        buffer.putInt(testMethod != null ? testMethod : 0);

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
        response.evalTaskId = readFixedString(buffer, 64);

        response.testPlatforms = readIntegerList(buffer);
        response.sonarTestLocation = readIntegerList(buffer);
        response.sonarTestTasks = readIntegerList(buffer);

        response.testMethod = buffer.getInt();

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

    // 辅助方法：写入整数列表
    private static void writeIntegerList(java.nio.ByteBuffer buffer, List<Integer> list) {
        int count = (list != null) ? list.size() : 0;
        ByteOrderUtils.writeUint16(buffer, count);
        if (list != null) {
            for (Integer val : list) {
                buffer.putInt(val);
            }
        }
    }

    // 辅助方法：读取整数列表
    private static List<Integer> readIntegerList(java.nio.ByteBuffer buffer) {
        int count = ByteOrderUtils.readUint16(buffer);
        java.util.ArrayList<Integer> list = new java.util.ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(buffer.getInt());
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

    public String getEvalTaskId() {
        return evalTaskId;
    }

    public void setEvalTaskId(String evalTaskId) {
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

    public Integer getTestMethod() {
        return testMethod;
    }

    public void setTestMethod(Integer testMethod) {
        this.testMethod = testMethod;
    }
}
