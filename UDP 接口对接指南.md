# UDP 接口对接指南

本此文档详细说明了 `EvaluationConfigRequest` 与 `EvaluationConfigResponse` 接口的对接规范、数据结构及注意事项，旨在帮助开发者快速、正确地完成 UDP 接口对接。

## 1. 协议基础说明

*   **传输协议**: UDP
*   **字节序**: **小端模式 (Little Endian)**。所有多字节整数（short, int, long, float, double）均需采用小端字节序传输。
*   **字符编码**: UTF-8
*   **默认端口** (参考配置):
    *   服务监听端口: 19210
    *   服务发送端口: 19211

### 1.1 分片传输协议 (Fragmentation Protocol)
**重要：所有 UDP 消息（无论大小）均需封装在分片结构中传输。**

为了支持大包传输并解决 UDP 包大小限制（MTU），所有业务消息（包括 `EvaluationConfigRequest` 和 `EvaluationConfigResponse`）在发送前都必须封装分片头。

#### 1.1.1 分片头部结构 (15 字节)
每个 UDP 数据包的最前端固定为 15 字节的分片头。

| 字段名 | 类型 | 长度 (Byte) | 说明 |
| :--- | :--- | :--- | :--- |
| **SessionId** | uint32 | 4 | 会话ID，用于标识同一组分片数据。同一次发送的所有分片此ID必须相同。 |
| **TotalPackets** | uint16 | 2 | 总分片数。如果不分片则为 1。 |
| **CurrentPacket** | uint16 | 2 | 当前分片序号（从 0 开始）。 |
| **CurrentSize** | uint16 | 2 | 当前分片的数据载荷长度（不含头部）。 |
| **Flags** | uint8 | 1 | 标志位（保留，目前填 0）。 |
| **Checksum** | uint32 | 4 | 校验和（CRC32或其他，目前可选填 0）。 |

#### 1.1.2 报文封装流程
发送任何业务消息时，请遵循以下步骤：

1.  **业务层编码**: 将业务对象（如 `EvaluationConfigResponse`）编码为字节数组。
    *   **注意**: 业务层编码的结果中**包含** `Type` 和 `RequestId` 字段。
    *   即：`业务数据 = [Type(2)] + [RequestId(8)] + [其他业务字段...]`
2.  **分片处理**: 将上述“业务数据”根据最大传输单元（如 1400 字节）进行切分。
3.  **添加头部**: 为每个切片添加 15 字节的 `FragmentHeader`。
4.  **最终 UDP 包结构**: `[FragmentHeader (15字节)] + [业务数据切片]`

**因此，Type 字段包含在 FragmentHeader 之后的数据载荷中（如果是第一个分片）。**

---

### 通用头部结构 (业务层)
在去除分片头（Fragment Header）重组后的完整业务数据中，均包含以下基础头部：

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
*   **注意**: 发送时需封装分片头。

#### 数据结构 (业务层)

| 字段名 | 类型 | 字节偏移 | 长度 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| **Type** | uint16 | 0 | 2 | 固定值 `2` (0x0002) |
| **RequestId** | uint64 | 2 | 8 | 请求ID |
| **ResponseTerminal** | uint16 | 10 | 2 | 响应终端标识 |

#### 有效对接案例 (Hex 示例)

假设 `RequestId = 100` (0x64), `ResponseTerminal = 1` (0x01)，且不分片。

**最终 UDP 发送数据 (Hex)** = `[分片头]` + `[业务数据]`

1.  **分片头 (15字节)**:
    *   SessionId: `88 88 88 88` (示例)
    *   TotalPackets: `01 00` (1)
    *   CurrentPacket: `00 00` (0)
    *   CurrentSize: `0C 00` (12字节)
    *   Flags: `00`
    *   Checksum: `00 00 00 00`
    *   Hex: `88 88 88 88 01 00 00 00 0C 00 00 00 00 00 00`

2.  **业务数据 (12字节)**:
    *   Type: `02 00`
    *   RequestId: `64 00 00 00 00 00 00 00`
    *   ResponseTerminal: `01 00`
    *   Hex: `02 00 64 00 00 00 00 00 00 00 01 00`

**实际发送包**:
```text
88 88 88 88 01 00 00 00 0C 00 00 00 00 00 00 02 00 64 00 00 00 00 00 00 00 01 00
```

---

### 2.2 评估配置响应 (EvaluationConfigResponse)

服务端返回的评估配置详情。

*   **消息类型 (Type)**: `2`
*   **长度**: 变长
*   **注意**: 接收后需先剥离分片头，重组后再解析。

#### 数据结构 (业务层)

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

假设业务层数据长度为 N 字节。

**封装过程**:
1.  生成 `FragmentHeader` (CurrentSize = N)。
2.  UDP Payload = `FragmentHeader.encode()` + `EvaluationConfigResponse.encode()`。

---

## 3. 对接关键点与避坑指南

为确保接口对接顺利，请务必仔细阅读以下要点：

### 3.1 字节序 (Endianness)
*   **⚠️ 重点**: 整个协议严格遵循 **Little Endian**。
*   **常见错误**: Java/C# 等语言的 `DataOutputStream` 或 `ByteBuffer` 默认通常是 Big Endian。务必手动设置为 Little Endian。
    *   *Java 示例*: `ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN)`

### 3.2 分片与 Type 字段
*   **Type 在哪？**: Type 字段属于业务数据的一部分，位于业务数据的最前端（Offset 0）。
*   **分片头**: 分片头是 UDP 包的物理头部，**不包含** Type 字段。
*   **解析顺序**:
    1.  读取 15 字节分片头，解析出 `TotalPackets`, `CurrentPacket` 等。
    2.  提取剩余的字节作为数据载荷。
    3.  如果是多包，缓存并根据 `SessionId` 和 `CurrentPacket` 重组完整数据。
    4.  从重组后的完整数据中，读取前 2 字节作为 `Type`，判断消息类型。

### 3.3 字符串处理
*   **定长字段**: 字符串字段 (`TestBackground`, `EvaluationPurpose`, `EvalTaskId`) 是定长的。
*   **填充规则**:
    *   写入时：如果字符串字节长度小于定义长度，**必须**在尾部填充 `0x00` 直到填满。
    *   读取时：读取指定长度的字节数组，并根据第一个 `0x00` 截断，或转为字符串后 `trim()` (注意 trim 去不掉中间的 NULL，建议检测 NULL 截断)。
*   **截断风险**: 如果字符串 UTF-8 编码后超过定义长度，**必须**截断，否则会破坏后续字段的偏移量。

### 3.4 列表 (List) 序列化
*   **结构**: 列表总是由 `Count (uint16)` + `Body (ItemType * Count)` 组成。
*   **空列表**: 即使列表为空，也必须写入 `0x00 00` (Count=0)，不能直接省略。
*   **边界检查**: 读取时请先读取 Count，并检查剩余字节数是否足够 `Count * ItemSize`，防止恶意数据导致内存溢出。

### 3.5 数据类型对齐
*   **Java 类型映射**:
    *   uint16 -> Java `short` (读取时 `Short.toUnsignedInt`) 或 `int`
    *   uint32 -> Java `int` (读取时 `Integer.toUnsignedLong` 如果可能溢出) 或 `long`
    *   uint64 -> Java `long`
*   **C++ 类型映射**: 直接对应 `uint16_t`, `uint32_t`, `uint64_t`。

### 3.6 调试建议
1.  **抓包验证**: 使用 Wireshark 抓取 UDP 包，确认 Hex 数据是否符合 Little Endian 预期。
2.  **日志**: 打印发送/接收的原始 byte 数组 Hex 字符串进行比对。
3.  **RequestId**: 确保 RequestId 在请求和响应中一致，用于异步匹配。
