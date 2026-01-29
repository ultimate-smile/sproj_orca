# UDP 接口对接指南

本此文档详细说明了 `EvaluationConfigRequest` 与 `EvaluationConfigResponse` 接口的对接规范、数据结构及注意事项，旨在帮助开发者快速、正确地完成 UDP 接口对接。

## 1. 协议基础说明

*   **传输协议**: UDP
*   **字节序**: **小端模式 (Little Endian)**。所有多字节整数（short, int, long, float, double）均需采用小端字节序传输。
*   **字符编码**: UTF-8
*   **默认端口** (参考配置):
    *   服务监听端口: 19210
    *   服务发送端口: 19211

### 1.1 传输层分片封装 (Transport Layer Fragmentation)
**⚠️ 重要**: 所有 UDP 数据包（无论是请求还是响应）在物理传输时，最外层都必须包含 **15字节** 的分片头部。业务数据作为 Payload 跟在分片头部之后。

**分片头部结构 (15 Byte)**

| 字段名 | 类型 | 长度 (Byte) | 说明 |
| :--- | :--- | :--- | :--- |
| SessionId | uint32 | 4 | 会话ID，同一组分片保持一致 |
| TotalPackets | uint16 | 2 | 总分片数 |
| CurrentPacket | uint16 | 2 | 当前分片序号 (从0开始) |
| CurrentSize | uint16 | 2 | 当前分片Payload长度 |
| Flags | uint8 | 1 | 标志位 (预留，默认为0) |
| Checksum | uint32 | 4 | 校验和 (预留，默认为0) |

> **注意**：即便数据很小不需要分片（TotalPackets=1），也必须包含此头部。

### 1.2 业务层通用头部 (Application Layer Header)
在分片头部之后，才是具体的业务数据。所有业务请求和响应共享相同的基础头部结构：

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

**发送数据 Payload (不含分片头)**:
```text
02 00 64 00 00 00 00 00 00 00 01 00
```

> **注意**: 实际通过 UDP 发送时，需在此数据前添加 15 字节的分片头。
> 例如 (SessionId=0x12345678, Total=1, Index=0, Size=12):
> `78 56 34 12` `01 00` `00 00` `0C 00` `00` `00 00 00 00` + `02 00 ...`

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

**数据流结构 (Payload 部分)**:

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

## 3. 对接关键点与避坑指南

为确保接口对接顺利，请务必仔细阅读以下要点：

### 3.1 字节序 (Endianness)
*   **⚠️ 重点**: 整个协议严格遵循 **Little Endian**。
*   **常见错误**: Java/C# 等语言的 `DataOutputStream` 或 `ByteBuffer` 默认通常是 Big Endian。务必手动设置为 Little Endian。
    *   *Java 示例*: `ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN)`

### 3.2 字符串处理
*   **定长字段**: 字符串字段 (`TestBackground`, `EvaluationPurpose`, `EvalTaskId`) 是定长的。
*   **填充规则**:
    *   写入时：如果字符串字节长度小于定义长度，**必须**在尾部填充 `0x00` 直到填满。
    *   读取时：读取指定长度的字节数组，并根据第一个 `0x00` 截断，或转为字符串后 `trim()` (注意 trim 去不掉中间的 NULL，建议检测 NULL 截断)。
*   **截断风险**: 如果字符串 UTF-8 编码后超过定义长度，**必须**截断，否则会破坏后续字段的偏移量。

### 3.3 列表 (List) 序列化
*   **结构**: 列表总是由 `Count (uint16)` + `Body (ItemType * Count)` 组成。
*   **空列表**: 即使列表为空，也必须写入 `0x00 00` (Count=0)，不能直接省略。
*   **边界检查**: 读取时请先读取 Count，并检查剩余字节数是否足够 `Count * ItemSize`，防止恶意数据导致内存溢出。

### 3.4 数据类型对齐
*   **Java 类型映射**:
    *   uint16 -> Java `short` (读取时 `Short.toUnsignedInt`) 或 `int`
    *   uint32 -> Java `int` (读取时 `Integer.toUnsignedLong` 如果可能溢出) 或 `long`
    *   uint64 -> Java `long`
*   **C++ 类型映射**: 直接对应 `uint16_t`, `uint32_t`, `uint64_t`。

### 3.5 调试建议
1.  **抓包验证**: 使用 Wireshark 抓取 UDP 包，确认 Hex 数据是否符合 Little Endian 预期。
2.  **日志**: 打印发送/接收的原始 byte 数组 Hex 字符串进行比对。
3.  **RequestId**: 确保 RequestId 在请求和响应中一致，用于异步匹配。
