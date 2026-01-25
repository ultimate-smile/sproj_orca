# 测试问题排查指南

## JUnit版本兼容性问题

如果遇到以下错误：
```
java.lang.NoSuchMethodError: 'java.lang.String org.junit.platform.engine.discovery.MethodSelector.getMethodParameterTypes()'
```

### 解决方案

#### 方案1：使用Maven运行测试（推荐）
```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=UdpRequestTest

# 运行特定测试方法
mvn test -Dtest=UdpRequestTest#testEncodeDecode
```

#### 方案2：在IDE中配置
1. **IntelliJ IDEA**:
   - File → Settings → Build, Execution, Deployment → Build Tools → Maven
   - 确保 "Use Maven wrapper" 已勾选
   - File → Invalidate Caches / Restart
   - 重新导入Maven项目：右键pom.xml → Maven → Reload Project

2. **确保使用Maven的依赖**:
   - Run → Edit Configurations
   - 选择JUnit配置
   - 在"Use classpath of module"中选择项目模块
   - 确保"Use classpath of module"已勾选

#### 方案3：更新IDE插件
- 确保IntelliJ IDEA版本支持JUnit 5
- 更新JUnit插件到最新版本

#### 方案4：清理并重新构建
```bash
mvn clean
mvn compile test-compile
```

### 验证修复
运行简单测试验证：
```bash
mvn test -Dtest=UdpRequestTest#testEncodeDecode
```

如果Maven测试通过但IDE测试失败，说明是IDE配置问题，使用Maven运行测试即可。
