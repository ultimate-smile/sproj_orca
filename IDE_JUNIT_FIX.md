# IDE JUnit 兼容性问题修复指南

## 问题描述
```
java.lang.NoSuchMethodError: 'java.lang.String org.junit.platform.engine.discovery.MethodSelector.getMethodParameterTypes()'
```

## 根本原因
Spring Boot 4.1.0-M1 使用 JUnit Jupiter 6.0.2，这是一个非常新的版本，IDE的JUnit插件可能尚未完全支持。

## 解决方案

### 方案1：使用Maven运行测试（最可靠）✅

**在IDE中：**
1. 打开 `Run` → `Edit Configurations`
2. 点击 `+` → `Maven`
3. 配置：
   - Name: `Maven Test`
   - Command line: `test`
   - Working directory: `$PROJECT_DIR$`
4. 运行此配置

**或在终端：**
```bash
mvn test
mvn test -Dtest=UdpRequestTest
```

### 方案2：重新导入Maven项目

1. **关闭所有运行配置**
2. **File** → **Invalidate Caches / Restart** → **Invalidate and Restart**
3. **重新导入Maven项目：**
   - 右键点击 `pom.xml`
   - 选择 `Maven` → `Reload Project`
4. **等待Maven依赖下载完成**
5. **再次尝试运行测试**

### 方案3：更新IDE和插件

1. **检查IntelliJ IDEA版本：**
   - `Help` → `About`
   - 确保使用 2023.3 或更新版本

2. **更新JUnit插件：**
   - `File` → `Settings` → `Plugins`
   - 搜索 "JUnit"
   - 更新到最新版本

3. **重启IDE**

### 方案4：使用Gradle Wrapper（如果Maven持续有问题）

如果Maven持续有问题，可以考虑切换到Gradle，但这不是必需的。

### 方案5：临时降级JUnit版本（不推荐）

如果以上方案都不行，可以在`pom.xml`中显式指定较旧的JUnit版本：

```xml
<properties>
    <junit-jupiter.version>5.10.2</junit-jupiter.version>
</properties>
```

但这可能导致与Spring Boot 4.1.0-M1的其他依赖不兼容。

## 验证修复

运行简单测试验证：
```bash
mvn test -Dtest=UdpRequestTest#testEncodeDecode
```

如果Maven测试通过，说明代码没问题，只是IDE配置问题。

## 推荐工作流程

1. **开发时：** 使用Maven运行测试（方案1）
2. **调试时：** 如果IDE测试失败，使用Maven测试配置
3. **CI/CD：** 使用Maven测试（这是标准做法）

## 已完成的配置

- ✅ 更新了`pom.xml`，显式指定JUnit版本
- ✅ 添加了`junit-platform-launcher`依赖
- ✅ 更新了IDE的JUnit配置
- ✅ 创建了Maven测试运行配置

## 如果问题仍然存在

1. 确认Maven测试是否通过：`mvn test`
2. 如果Maven测试通过，这是IDE问题，使用Maven运行测试即可
3. 如果Maven测试也失败，检查错误信息并修复代码问题
