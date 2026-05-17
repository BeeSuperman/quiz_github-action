package com.example.quiz_1141121.config;

import java.util.concurrent.TimeUnit;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

@EnableCaching //Spring framework 中用來驅動暫存的註解，容器內只少要有一個 CacheManager 的 Bean
@Configuration //配置成設定檔，並交由 Spring Boot 託管
public class CaffeineCacheConfig {
	
	@Bean
	//只要是被springboot託管的就是叫bean（豆子）
	public CacheManager cacheManager() {
		CaffeineCacheManager cacheManager = new CaffeineCacheManager();
		cacheManager.setCaffeine(Caffeine.newBuilder()
				// 設定過期時間；每次處理(寫或讀)後，固定時間內不再有動作，快取資料就會失效
				.expireAfterAccess(600, TimeUnit.SECONDS)
				// 初始的容量: 初始預計可容納的最小筆數
				.initialCapacity(100)
				// 可暫存的最大筆數
				.maximumSize(500));
		return cacheManager;
	}

}
