# 单元测试说明

## 测试结构

### 1. 协议层测试
- `UdpRequestTest.java` - UDP请求编解码测试
- `UdpResponseTest.java` - UDP响应编解码测试
- `FragmentHeaderTest.java` - 分片头部编解码测试
- `FragmentReassemblerTest.java` - 分片重组测试
- `RealWorldUdpPacketTest.java` - 真实UDP报文测试

### 2. 服务层测试
- `UdpServiceIntegrationTest.java` - UDP服务集成测试（模拟UDP服务器）

### 3. WebSocket测试
- `WebSocketIntegrationTest.java` - WebSocket集成测试

### 4. 端到端测试
- `EndToEndTest.java` - 完整流程测试（WebSocket -> UDP -> WebSocket）

## 运行测试

### 运行所有测试
```bash
mvn test
```

### 运行特定测试类
```bash
mvn test -Dtest=UdpRequestTest
mvn test -Dtest=EndToEndTest
```

### 运行特定测试方法
```bash
mvn test -Dtest=UdpRequestTest#testEncodeDecode
```

## 测试场景

### 真实场景数据
测试使用真实的北京地区坐标：
- 天安门：116.397128, 39.909604
- 故宫：116.397026, 39.918058

### 测试覆盖
1. ✅ UDP请求编码/解码
2. ✅ UDP响应编码/解码（单点/多点）
3. ✅ 分片头部编解码
4. ✅ 分片拆分和重组
5. ✅ UDP服务请求-响应流程
6. ✅ WebSocket请求/响应转换
7. ✅ 端到端完整流程

## 注意事项

1. **端口冲突**：集成测试使用非标准端口（19212-19217）避免与生产环境冲突
2. **异步测试**：UDP服务测试使用CompletableFuture，有超时设置
3. **模拟服务器**：集成测试包含模拟UDP服务器，自动启动和停止
