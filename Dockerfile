# 1. 基礎映像檔改為 17
FROM eclipse-temurin:17-jdk-alpine

# 2. 設定工作目錄
WORKDIR /app

# 3. 將 Gradle 打包好的 JAR 複製到容器中
# 這裡注意：路徑必須指向 build/libs/
COPY build/libs/quiz_1141121_123-0.0.1-SNAPSHOT.jar app.jar


# 4. 暴露應用程式埠號 (Spring Boot 預設為 8080)
EXPOSE 8080

# 5. 啟動指令
ENTRYPOINT ["java", "-jar", "app.jar"]