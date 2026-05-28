package com.example.quiz_1141121.config;

import org.springframework.context.annotation.Configuration;

// 原 Caffeine 本地快取設定，已改用 Redis 分散式快取（見 RedisConfig.java）
// Caffeine 依賴已從 build.gradle 移除，此類保留作版本對照用
@Configuration
public class CaffeineCacheConfig {

}
