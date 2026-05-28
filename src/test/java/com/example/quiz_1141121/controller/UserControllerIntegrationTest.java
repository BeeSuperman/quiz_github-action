package com.example.quiz_1141121.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
// @ActiveProfiles("integrationtest")：啟用整合測試專用 profile
// 載入 src/test/resources/application-integrationtest.properties
// 這樣 main 的 application.properties 不會被覆蓋（aaa.bbb 等屬性仍然存在）
// @ServiceConnection 提供 Testcontainers MySQL 的資料庫連線，不需要 dev datasource
@ActiveProfiles("integrationtest")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UserControllerIntegrationTest {

    // @LocalServerPort：注入 Spring Boot 隨機選的 port 號碼
    // 例如 53421，這樣就不會和本機的 8080 衝突
    @LocalServerPort
    private int port;

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    // Redis 用 GenericContainer，Testcontainers 沒有專屬的 Redis 模組
    // withExposedPorts：告訴 Testcontainers 要對外開放哪個 port
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.2-alpine")
            .withExposedPorts(6379);

    // @ServiceConnection 不支援 GenericContainer，所以要用 @DynamicPropertySource
    // 讓 Spring Boot 啟動時動態讀到 Testcontainers 起的 Redis 的實際 host 和 port
    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    // 用標準 RestTemplate，Spring Boot 4.x 移除了 TestRestTemplate
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate(new SimpleClientHttpRequestFactory());
        // 關閉預設的錯誤拋出行為
        // 預設 RestTemplate 收到 4xx/5xx 會丟 Exception，我們要自己驗證狀態碼
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(org.springframework.http.HttpStatusCode statusCode) {
                return false; // 不管什麼狀態碼都不丟 Exception，讓測試自己判斷
            }
        });
    }

    // 組合完整 URL，例如 http://localhost:53421/user/register
    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    // 建立帶有 Content-Type: application/json 的 request
    private HttpEntity<String> jsonRequest(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    // ============================
    // /user/register 測試
    // ============================

    @Test
    void register_whenAllParamsValid_shouldReturn200() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
            url("/user/register"),
            jsonRequest("""
                {
                    "email": "test@example.com",
                    "password": "Test1234",
                    "name": "整合測試用戶",
                    "phone": "0912345678",
                    "age": 25
                }
                """),
            Map.class
        );

        assertEquals(200, response.getStatusCode().value());
        assertEquals(200, response.getBody().get("code"));
    }

    @Test
    void register_whenEmailAlreadyExists_shouldReturnError() {
        String body = """
            {
                "email": "duplicate@example.com",
                "password": "Test1234",
                "name": "用戶A"
            }
            """;

        // 第一次：成功
        restTemplate.postForEntity(url("/user/register"), jsonRequest(body), Map.class);

        // 第二次：同 email，應被擋住
        ResponseEntity<Map> response = restTemplate.postForEntity(
            url("/user/register"), jsonRequest(body), Map.class);

        assertEquals(404, response.getBody().get("code")); // USER_EMAIL_EXISTED = 404
    }

    @Test
    void register_whenEmailIsEmpty_shouldReturnError() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
            url("/user/register"),
            jsonRequest("""
                {
                    "email": "",
                    "password": "Test1234",
                    "name": "用戶B"
                }
                """),
            Map.class
        );

        assertEquals(404, response.getBody().get("code")); // USER_EMAIL_ERROR = 404
    }

    // ============================
    // /user/login 測試
    // ============================

    @Test
    void login_whenCredentialsValid_shouldReturnTokenAndUser() {
        // 先註冊
        restTemplate.postForEntity(url("/user/register"),
            jsonRequest("""
                {
                    "email": "login@example.com",
                    "password": "Test1234",
                    "name": "登入測試用戶"
                }
                """),
            Map.class);

        // 再登入
        ResponseEntity<Map> response = restTemplate.postForEntity(
            url("/user/login"),
            jsonRequest("""
                {
                    "email": "login@example.com",
                    "password": "Test1234"
                }
                """),
            Map.class
        );

        Map body = response.getBody();
        assertEquals(200, body.get("code"));
        assertNotNull(body.get("token"));                   // token 不為空
        assertNull(((Map) body.get("user")).get("password")); // 密碼不回傳前端
    }

    @Test
    void login_whenAccountNotExist_shouldReturnError() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
            url("/user/login"),
            jsonRequest("""
                {
                    "email": "notexist@example.com",
                    "password": "Test1234"
                }
                """),
            Map.class
        );

        assertEquals(404, response.getBody().get("code")); // USER_NOT_FOUND = 404
    }

    @Test
    void login_whenPasswordWrong_shouldReturnError() {
        // 先註冊
        restTemplate.postForEntity(url("/user/register"),
            jsonRequest("""
                {
                    "email": "wrongpw@example.com",
                    "password": "CorrectPw123",
                    "name": "用戶C"
                }
                """),
            Map.class);

        // 用錯誤密碼登入
        ResponseEntity<Map> response = restTemplate.postForEntity(
            url("/user/login"),
            jsonRequest("""
                {
                    "email": "wrongpw@example.com",
                    "password": "WrongPw999"
                }
                """),
            Map.class
        );

        assertEquals(400, response.getBody().get("code"));
    }
}
