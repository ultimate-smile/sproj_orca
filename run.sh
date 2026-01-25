#!/bin/bash
# 运行脚本 - 如果Maven权限问题已解决，使用此脚本

cd "$(dirname "$0")"

echo "正在编译项目..."
mvn clean compile -q

if [ $? -eq 0 ]; then
    echo "编译成功，正在启动应用..."
    mvn spring-boot:run
else
    echo "编译失败，请检查错误信息"
    exit 1
fi
