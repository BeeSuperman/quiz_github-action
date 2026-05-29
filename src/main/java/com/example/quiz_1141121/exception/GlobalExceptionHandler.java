package com.example.quiz_1141121.exception;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 1. @RestControllerAdvice 包含了 @ControllerAdvice 和 @ResponseBody <br>
 * 1.1. @ControllerAdvice: 使用於 class 的註釋，用來定義「全域」的例外處理策略<br>
 * 1.2. @ResponseBody: 這是一個告訴 Spring 每次 response 都需要將返回的物件序列化為 JSON 或 XML 格式，
 *                   然後放入 HTTP 回應的內容中<br>
 * 2. @ExceptionHandler: 使用於 method 中的註釋，提供了在 controller 處理例外的功能<br>
 * 3. ResponseEntity: 可以做為 controller 的返回值
 */
@RestControllerAdvice //用來表示這是一個全域的 REST 例外處理器
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	// @Valid 配合 Spring 會抛出 MethodArgumentNotValidException 異常；所以要針對此 Exception 處理
	@ExceptionHandler({MethodArgumentNotValidException.class})
	public ResponseEntity<Map<String, Object>> paramExceptionHandler(MethodArgumentNotValidException e) {
		String errorMsg = e.getAllErrors().get(0).getDefaultMessage();
		// WARN：前端傳了不合法的參數，後端沒壞，用 WARN 而非 ERROR
		log.warn("參數驗證失敗：{}", errorMsg);

		Map<String, Object> errorMap = new HashMap<>();
		errorMap.put("code", HttpStatus.BAD_REQUEST.value());
		errorMap.put("message", errorMsg);
		return ResponseEntity.badRequest().body(errorMap);
	}

	@ExceptionHandler({SQLException.class})
	public ResponseEntity<Map<String, Object>> handleSQLException(SQLException e) {
		// ERROR：資料庫操作失敗是嚴重問題，第三個參數 e 讓 Logback 輸出完整 stack trace
		log.error("SQL 執行異常：{}", e.getMessage(), e);

		Map<String, Object> errorMap = new HashMap<>();
		errorMap.put("code", HttpStatus.INTERNAL_SERVER_ERROR.value());
		errorMap.put("message", e.getMessage());
		return ResponseEntity.internalServerError().body(errorMap);
	}

	@ExceptionHandler({Exception.class})
	public ResponseEntity<Map<String, Object>> handleException(Exception e) {
		// ERROR：未預期錯誤，需要完整 stack trace 才能除錯
		log.error("未預期的異常：{}", e.getMessage(), e);

		Map<String, Object> errorMap = new HashMap<>();
		errorMap.put("code", HttpStatus.INTERNAL_SERVER_ERROR.value());
		errorMap.put("message", e.getMessage());
		return ResponseEntity.internalServerError().body(errorMap);
	}
	
}
