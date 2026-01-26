# Docker 构建指南 (Mac M4 -> Windows)

由于您的开发环境是 Mac M4 (ARM64 架构)，而目标运行环境是 Windows (通常是 AMD64/x86_64 架构)，直接构建的镜像可能会导致在 Windows 上无法运行。

以下是构建兼容 Windows (AMD64) 镜像的正确步骤。

## 1. 准备工作

已创建以下文件：
- `Dockerfile`: 多阶段构建文件。
- `docker-compose.yml`: 编排文件，已指定 `platform: linux/amd64`。

## 2. 构建跨平台镜像 (核心步骤)

在您的 Mac M4 终端中，**不要**直接运行简单的 `docker build`。请使用 `buildx` 来指定目标平台。

### 方案 A：使用 Docker Compose (推荐)

由于 `docker-compose.yml` 中已经配置了 `platform: linux/amd64`，您可以直接运行：

```bash
docker-compose up --build
```

*注意：这会在您的 Mac 上通过 Rosetta 模拟运行 AMD64 容器，速度可能稍慢，但这能确保生成的镜像和容器是 AMD64 架构的。*

### 方案 B：手动构建并导出镜像 (用于分发)

如果您需要将镜像打包发给 Windows 机器使用：

1.  **构建 AMD64 镜像**：
    ```bash
    docker buildx build --platform linux/amd64 -t orca-udp:latest --load .
    ```
    *解释：`--platform linux/amd64` 强制生成兼容 Windows/Intel 的架构，`--load` 将镜像加载到本地 Docker 守护进程。*

2.  **保存为 Tar 包**：
    ```bash
    docker save -o orca-udp.tar orca-udp:latest
    ```

3.  **在 Windows 上加载**：
    将 `orca-udp.tar` 复制到 Windows 机器，运行：
    ```powershell
    docker load -i orca-udp.tar
    docker run -d -p 8080:8080 -p 19210:19210/udp -p 19211:19211/udp orca-udp:latest
    ```

## 3. 注意事项

1.  **UDP 端口映射**：
    在 `docker-compose.yml` 中，UDP 端口必须显式标记 `/udp`，例如 `19210:19210/udp`。

2.  **网络通信**：
    - 容器内的 `0.0.0.0` 对应容器自身的网络接口。
    - 如果容器需要向 Windows 宿主机发送 UDP 数据 (19211端口)，目标 IP 不能是 `127.0.0.1` (这代表容器自己)。
    - 使用 `host.docker.internal` (Docker Desktop 专用 DNS) 来指代宿主机。

3.  **性能**：
    在 Mac M4 上运行 AMD64 容器会有性能损耗（转译开销），但在 Windows 上运行时将是原生的，性能最佳。
