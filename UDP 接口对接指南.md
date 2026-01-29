# UDP 接口对接指南

本此文档详细说明了 `EvaluationConfigRequest` 与 `EvaluationConfigResponse` 接口的对接规范、数据结构及注意事项，旨在帮助开发者快速、正确地完成 UDP 接口对接。

## 1. 协议基础说明

*   **传输协议**: UDP
*   **字节序**: **小端模式 (Little Endian)**。所有多字节整数（short, int, long, float, double）均需采用小端字节序传输。
*   **字符编码**: UTF-8
*   **默认端口** (参考配置):
    *   服务监听端口: 19210
    *   服务发送端口: 19211

### 通用头部结构
所有请求和响应共享相同的基础头部结构：

| 字段名 | 类型 | 长度 (Byte) | 说明 |
| :--- | :--- | :--- | :--- |
| Type | uint16 | 2 | 消息类型标识 |
| RequestId | uint64 | 8 | 请求唯一ID，用于匹配请求与响应 |

---

## 2. 接口详解

### 2.1 评估配置请求 (EvaluationConfigRequest)

用于客户端向服务端请求评估配置信息。

*   **消息类型 (Type)**: `2`
*   **总长度**: 12 字节

#### 数据结构

| 字段名 | 类型 | 字节偏移 | 长度 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| **Type** | uint16 | 0 | 2 | 固定值 `2` (0x0002) |
| **RequestId** | uint64 | 2 | 8 | 请求ID |
| **ResponseTerminal** | uint16 | 10 | 2 | 响应终端标识 |

#### 有效对接案例 (Hex 示例)

假设 `RequestId = 100` (0x64), `ResponseTerminal = 1` (0x01)

**发送数据 (Hex)**:
```text
02 00 64 00 00 00 00 00 00 00 01 00
```

**解析**:
*   `02 00`: Type = 2 (Little Endian)
*   `64 00 00 00 00 00 00 00`: RequestId = 100
*   `01 00`: ResponseTerminal = 1

---

### 2.2 评估配置响应 (EvaluationConfigResponse)

服务端返回的评估配置详情。

*   **消息类型 (Type)**: `2`
*   **长度**: 变长

#### 数据结构

| 字段名 | 类型 | 长度 (Byte) | 说明 |
| :--- | :--- | :--- | :--- |
| **Type** | uint16 | 2 | 固定值 `2` |
| **RequestId** | uint64 | 8 | 对应请求的 ID |
| **TestBackground** | String | 128 | 定长字符串，不足补0，UTF-8 |
| **EvaluationPurpose** | String | 128 | 定长字符串，不足补0，UTF-8 |
| **EvalTaskId** | String | 64 | 定长字符串，不足补0，UTF-8 |
| **TestPlatformsCount** | uint16 | 2 | 平台列表元素个数 (N) |
| **TestPlatforms** | int32[] | 4 * N | 平台ID列表 |
| **SonarTestLocationCount**| uint16 | 2 | 位置列表元素个数 (M) |
| **SonarTestLocation** | int32[] | 4 * M | 位置ID列表 |
| **SonarTestTasksCount** | uint16 | 2 | 任务列表元素个数 (K) |
| **SonarTestTasks** | int32[] | 4 * K | 任务ID列表 |
| **TestMethod** | int32 | 4 | 评估方式 (0, 1, 2) |

#### 有效对接案例 (Hex 示例)

假设数据如下：
*   RequestId: 100
*   TestBackground: "Test"
*   EvaluationPurpose: "Purpose"
*   EvalTaskId: "Task01"
*   TestPlatforms: [10, 20]
*   SonarTestLocation: [] (空)
*   SonarTestTasks: [99]
*   TestMethod: 1

**数据流结构**:

1.  **Header**: `02 00` (Type) + `64 00 00 00 00 00 00 00` (ReqId)
2.  **Strings**:
    *   `54 65 73 74 00 ...` (TestBackground, "Test" + 124个0x00)
    *   `50 75 72 70 6F 73 65 00 ...` (EvaluationPurpose, "Purpose" + 121个0x00)
    *   `54 61 73 6B 30 31 00 ...` (EvalTaskId, "Task01" + 58个0x00)
3.  **Lists**:
    *   Platforms: `02 00` (Count=2) + `0A 00 00 00` (10) + `14 00 00 00` (20)
    *   Location: `00 00` (Count=0)
    *   Tasks: `01 00` (Count=1) + `63 00 00 00` (99)
4.  **Tail**: `01 00 00 00` (TestMethod=1)

---

## 3. 分片传输协议 (Fragmentation Protocol)

为了支持大数据包传输，协议定义了分片机制。当数据包较大时，可能会被拆分成多个分片发送。每个分片都包含一个 `FragmentHeader`，后跟实际的数据负载。

**注意**: 即使数据不需要分片（只有1个包），也可能包裹 `FragmentHeader` 发送（此时 `TotalPackets=1`, `CurrentPacket=0`）。建议接收端统一按分片逻辑处理。

### 3.1 分片头结构

分片头固定长度为 **15 字节**，位于每个 UDP 包的最前端。

| 字段名 | 类型 | 长度 (Byte) | 说明 |
| :--- | :--- | :--- | :--- |
| **SessionId** | uint32 | 4 | 会话/组ID，用于标识属于同一完整消息的所有分片 |
| **TotalPackets** | uint16 | 2 | 分片总数 (N) |
| **CurrentPacket** | uint16 | 2 | 当前分片序号 (0 ~ N-1) |
| **CurrentSize** | uint16 | 2 | 当前分片中 **负载数据** 的长度 (不含分片头) |
| **Flags** | uint8 | 1 | 标志位 (保留，默认 0) |
| **Checksum** | uint32 | 4 | 校验和 (保留，默认 0) |

### 3.2 封装与使用示例

以下 Java 伪代码演示了如何生成响应并封装分片头：

```java
// 1. 生成业务数据 (以 EvaluationConfigResponse 为例)
EvaluationConfigResponse response = new EvaluationConfigResponse();
response.setRequestId(2002L);
// ... 设置其他字段 ...
byte[] respBytes = response.encode();

// 2. 封装分片头 (假设数据较小，不需拆分，仅封装1个分片)
// 参数: sessionId=88888, totalPackets=1, currentPacket=0, currentSize=respBytes.length
FragmentHeader header = new FragmentHeader(88888L, 1, 0, respBytes.length);
byte[] headerBytes = header.encode();

// 3. 组合最终 UDP 数据包
byte[] fullPacket = new byte[headerBytes.length + respBytes.length];
System.arraycopy(headerBytes, 0, fullPacket, 0, headerBytes.length);
System.arraycopy(respBytes, 0, fullPacket, headerBytes.length, respBytes.length);

// 4. 发送 fullPacket
System.out.println("[发送报文] 含分片头 Total: " + fullPacket.length + " bytes");
```

### 3.3 实现注意事项

1.  **SessionId 生成**: 每次发送新的完整消息时，应生成一个新的 `SessionId`（例如自增或随机），以便接收端区分交叉到达的不同消息分片。
2.  **重组逻辑**:
    *   接收端应维护一个缓存池，以 `SessionId` 为 Key。
    *   收到分片后，根据 `TotalPackets` 检查是否已收齐所有分片。
    *   收齐后，按 `CurrentPacket` 排序并拼接 `Payload`，即可还原原始业务数据。
3.  **小端字节序**: 分片头中的 `SessionId`, `TotalPackets` 等字段同样遵循 **Little Endian**。
4.  **UDP 限制**: 建议单个 UDP 包大小（分片头+负载）控制在 MTU 范围内（如 1400 字节），以避免 IP 层分片。

---

## 4. 对接关键点与避坑指南

为确保接口对接顺利，请务必仔细阅读以下要点：

### 4.1 字节序 (Endianness)
*   **⚠️ 重点**: 整个协议严格遵循 **Little Endian**。
*   **常见错误**: Java/C# 等语言的 `DataOutputStream` 或 `ByteBuffer` 默认通常是 Big Endian。务必手动设置为 Little Endian。
    *   *Java 示例*: `ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN)`

### 4.2 字符串处理
*   **定长字段**: 字符串字段 (`TestBackground`, `EvaluationPurpose`, `EvalTaskId`) 是定长的。
*   **填充规则**:
    *   写入时：如果字符串字节长度小于定义长度，**必须**在尾部填充 `0x00` 直到填满。
    *   读取时：读取指定长度的字节数组，并根据第一个 `0x00` 截断，或转为字符串后 `trim()` (注意 trim 去不掉中间的 NULL，建议检测 NULL 截断)。
*   **截断风险**: 如果字符串 UTF-8 编码后超过定义长度，**必须**截断，否则会破坏后续字段的偏移量。

### 4.3 列表 (List) 序列化
*   **结构**: 列表总是由 `Count (uint16)` + `Body (ItemType * Count)` 组成。
*   **空列表**: 即使列表为空，也必须写入 `0x00 00` (Count=0)，不能直接省略。
*   **边界检查**: 读取时请先读取 Count，并检查剩余字节数是否足够 `Count * ItemSize`，防止恶意数据导致内存溢出。

### 4.4 数据类型对齐
*   **Java 类型映射**:
    *   uint16 -> Java `short` (读取时 `Short.toUnsignedInt`) 或 `int`
    *   uint32 -> Java `int` (读取时 `Integer.toUnsignedLong` 如果可能溢出) 或 `long`
    *   uint64 -> Java `long`
*   **C++ 类型映射**: 直接对应 `uint16_t`, `uint32_t`, `uint64_t`。

### 4.5 调试建议
1.  **抓包验证**: 使用 Wireshark 抓取 UDP 包，确认 Hex 数据是否符合 Little Endian 预期。
2.  **日志**: 打印发送/接收的原始 byte 数组 Hex 字符串进行比对。
3.  **RequestId**: 确保 RequestId 在请求和响应中一致，用于异步匹配。
