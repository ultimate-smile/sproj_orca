FROM eclipse-temurin:17-jdk
# 在docker容器中自建容器卷,用于数据保存和持久化工作
WORKDIR /app

ADD ./target/*.jar app.jar

# Expose ports: 8080 (Web/WS), 19210 (UDP Listen), 19211 (UDP Send target)
EXPOSE 8080
EXPOSE 19210/udp
EXPOSE 19211/udp

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
