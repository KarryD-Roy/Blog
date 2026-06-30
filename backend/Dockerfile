# ============================================
# 后端 Spring Boot - 多阶段构建
# ============================================

# ---- Stage 1: Maven 构建 ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# 先复制 pom.xml，利用 Docker 层缓存加速依赖下载
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源码并打包
COPY src ./src
RUN mvn package -DskipTests -B

# ---- Stage 2: 运行时镜像 ----
FROM eclipse-temurin:17-jre
WORKDIR /app

# 创建非 root 用户（安全性最佳实践）
RUN groupadd -r blog && useradd -r -g blog blog

# 从构建阶段复制 JAR
COPY --from=build /build/target/*.jar app.jar

# 赋予权限
RUN chown -R blog:blog /app

# 切换到非 root 用户
USER blog

EXPOSE 8080

ENTRYPOINT ["java", \
    "-jar", \
    "app.jar"]
