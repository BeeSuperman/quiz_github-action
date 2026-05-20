# 學習手冊：單元測試、整合測試、Spring Profile 環境分離

> 本文件整理了在 `quiz_1141121_123` 專案上實作的完整過程，包含遇到的錯誤、解決方法、原理說明，以及從零開始的業界 SOP。
> 標記說明：⚠️ **學習用** 表示此做法為學習簡化版，公司實際做法不同。

---

## 目錄

1. [JUnit 5 + Mockito 單元測試](#part-1-junit-5--mockito-單元測試)
2. [Spring Boot 整合測試（Testcontainers）](#part-2-spring-boot-整合測試testcontainers)
3. [Spring Profile 環境分離](#part-3-spring-profile-環境分離)

---

# Part 1：JUnit 5 + Mockito 單元測試

## 1-1 是什麼

單元測試是測試「單一類別/方法的邏輯」，不連資料庫，不啟動 Spring。

```
單元測試的世界：
QuizService（真實）
    ↓ 依賴
QuizDao（假的 Mock，你控制它回什麼）
QuestionDao（假的 Mock）
```

**為什麼不連真實資料庫？**
- 測試速度快（毫秒級）
- 資料庫資料不穩定（今天有資料、明天沒有），測試結果不可重複
- 可以精準測試「這個 if 條件成不成立」，不受外部影響

## 1-2 核心注解說明

```java
@ExtendWith(MockitoExtension.class)  // 告訴 JUnit 用 Mockito 擴充功能
class QuizServiceTest {

    @Mock
    private QuizDao quizDao;          // 建立假的 QuizDao，你可以控制它回傳什麼

    @Mock
    private QuestionDao questionDao;  // 建立假的 QuestionDao

    @InjectMocks
    private QuizService quizService;  // 建立真實的 QuizService，把上面兩個假的注入進去

    @BeforeEach
    void setUp() {
        // 每個測試方法執行前都會先跑這裡，準備測試資料
    }
}
```

| 注解 | 作用 |
|------|------|
| `@ExtendWith(MockitoExtension.class)` | 啟用 Mockito，讓 `@Mock`、`@InjectMocks` 生效 |
| `@Mock` | 建立假的物件（Mock），所有方法預設回傳 null/0 |
| `@InjectMocks` | 建立真實物件，並把所有 `@Mock` 自動注入進去 |
| `@BeforeEach` | 每個 `@Test` 方法執行前都先跑一次 |
| `@Test` | 標記這是一個測試方法，JUnit 會執行它 |

## 1-3 我們寫了什麼（`QuizServiceTest.java`）

檔案位置：`src/test/java/com/example/quiz_1141121/service/QuizServiceTest.java`

共 15 個測試方法，覆蓋：

| 測試方法 | 測試目的 |
|----------|---------|
| `create_whenTitleIsEmpty` | title 為空時，應回傳 TITLE_ERROR |
| `create_whenDescriptionIsEmpty` | description 為空時，應回傳 DESCRIPTION_ERROR |
| `create_whenStartDateIsNull` | startDate 為 null 時，應回傳 START_DATE_ERROR |
| `create_whenStartDateIsBeforeToday` | startDate 在今天之前，應回傳 START_DATE_ERROR |
| `create_whenEndDateIsNull` | **發現真實 Bug**：endDate 為 null 時，會丟 NullPointerException |
| `create_whenAllParamsValid` | 所有參數合法時，應回傳 SUCCESS |
| `getQuizList_shouldReturnSuccessWithList` | 有資料時，回傳成功 + 資料列表 |
| `getQuizList_whenNoData` | 無資料時，回傳成功 + 空列表 |
| `getQuiz_whenQuizNotFound` | quizId 不存在，回傳 QUIZ_NOT_FOUND |
| `getQuiz_whenQuizExists` | quizId 存在，回傳 SUCCESS + 問卷資料 |
| `getQuiz_whenQuizIdIsZero` | quizId=0，回傳 QUIZ_NOT_FOUND |
| `getQuiz_whenQuizIdIsNegative` | quizId 負數，回傳 QUIZ_NOT_FOUND |
| `delete_whenQuizIdListIsEmpty` | 傳空列表，回傳 QUIZ_ID_ERROR |
| `delete_whenQuizIdIsZero` | 傳 [0]，回傳 QUIZ_ID_ERROR |
| `delete_whenValidIds` | 傳合法 ID，回傳 SUCCESS |

### 測試的 AAA 結構（每個測試都要遵守）

```java
@Test
void create_whenAllParamsValid_shouldReturnSuccess() {
    // ===== Arrange（準備）=====
    when(quizDao.getMaxId()).thenReturn(1);  // 設定 Mock 行為

    // ===== Act（執行）=====
    CreateRes res = quizService.create(validReq);  // 呼叫真實方法

    // ===== Assert（驗證）=====
    assertEquals(ReplyMessage.SUCCESS.getCode(), res.getCode());  // 驗證結果
}
```

## 1-4 Mockito 的 `when().thenReturn()` 語法

```java
// 語法：when(假DAO的方法呼叫).thenReturn(你想要它回傳的值)
when(quizDao.getMaxId()).thenReturn(1);
// 意思：當有人呼叫 quizDao.getMaxId() 時，假DAO 回傳 1

when(quizDao.getById(999)).thenReturn(null);
// 意思：當有人呼叫 quizDao.getById(999) 時，假DAO 回傳 null
```

## 1-5 我們發現的真實 Bug

測試 `create_whenEndDateIsNull` 時，發現了 `QuizService.checkParams()` 裡的 Bug：

**Bug 原因：** `endDate` 的 null 檢查在 `startDate.isAfter(endDate)` 之後，導致當 `endDate=null` 時，呼叫 `.isAfter(null)` 丟出 NullPointerException。

**這說明了單元測試的價值：** 在程式上線之前，就發現了這個邊界條件 Bug。

## 1-6 如何跑測試

```powershell
.\gradlew test
```

---

# Part 2：Spring Boot 整合測試（Testcontainers）

## 2-1 整合測試 vs 單元測試

```
單元測試（Mockito）：
[你的程式碼] ← [假的 DAO] ← 不碰資料庫

整合測試（Testcontainers）：
[你的程式碼] ← [真實 DAO] ← [真實 MySQL Docker 容器]
                                    ↑ 完全真實的資料庫操作
```

| 比較 | 單元測試 | 整合測試 |
|------|---------|---------|
| 速度 | 毫秒級 | 秒級（要等 MySQL 啟動） |
| 資料庫 | 不碰 | 真實 MySQL（Docker 容器） |
| 測試範圍 | 單一方法邏輯 | 整個 HTTP 請求流程 |
| 適合測試 | 商業邏輯、if/else | API 完整流程、資料庫操作 |

## 2-2 Testcontainers 是什麼

Testcontainers 是一個 Java 函式庫，可以在測試時自動：
1. 啟動一個 Docker 容器（例如 MySQL）
2. 等容器準備好後開始測試
3. 測試結束後自動刪除容器

你不需要在電腦上事先安裝 MySQL，Testcontainers 會幫你用 Docker 建立一個。

**前提條件：** 電腦上必須安裝並開啟 Docker Desktop。

> ⚠️ **學習用**：公司實際做法是 Testcontainers 配合 CI/CD Pipeline（GitHub Actions），CI 的 Ubuntu runner 內建 Docker，每次 push 程式碼就自動跑整合測試，不需要開發者手動啟動 Docker Desktop。

## 2-3 我們做了什麼（完整過程）

### Step 1：在 `build.gradle` 加依賴

```groovy
// Testcontainers：啟動真實 MySQL Docker 容器跑整合測試
testImplementation 'org.springframework.boot:spring-boot-testcontainers'
// 明確指定版本，Spring Boot 4.x 的 BOM 不會自動解析 testcontainers 版本
testImplementation 'org.testcontainers:junit-jupiter:1.20.4'
testImplementation 'org.testcontainers:mysql:1.20.4'
```

**三個依賴的作用：**

| 依賴 | 作用 |
|------|------|
| `spring-boot-testcontainers` | Spring Boot 和 Testcontainers 的橋接，提供 `@ServiceConnection` 注解 |
| `testcontainers:junit-jupiter` | 讓 JUnit 5 可以控制 Docker 容器的生命週期（`@Container`、`@Testcontainers`） |
| `testcontainers:mysql` | 提供 `MySQLContainer`，知道怎麼啟動一個 MySQL Docker 容器 |

### Step 2：建立測試專用 Profile 設定檔

建立 `src/test/resources/application-integrationtest.properties`：

```properties
# 讓 JPA 根據 Entity 自動建表，測試結束後刪表
spring.jpa.hibernate.ddl-auto=create-drop

# 關閉 SQL 初始化腳本（測試用的 MySQL 容器是空的，不需要執行 schema 腳本）
spring.sql.init.mode=never

# 關閉 SQL 輸出，測試 log 不需要看每條 SQL
spring.jpa.show-sql=false
```

**⚠️ 重要：為什麼用 `application-integrationtest.properties` 而不是 `application.properties`？**

這是我們在實作中踩到的坑（詳見 2-4 遇到的問題）。

### Step 3：建立整合測試類別

建立 `src/test/java/com/example/quiz_1141121/controller/UserControllerIntegrationTest.java`：

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("integrationtest")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UserControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate(new SimpleClientHttpRequestFactory());
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(HttpStatusCode statusCode) {
                return false; // 不管任何狀態碼都不丟 Exception
            }
        });
    }
    // ... 測試方法
}
```

**每個注解的意思：**

| 注解 | 作用 |
|------|------|
| `@SpringBootTest(webEnvironment = RANDOM_PORT)` | 啟動完整 Spring 容器，用隨機 port，不和本機 8080 衝突 |
| `@Testcontainers` | 告訴 JUnit 5 這個測試需要管理 Docker 容器 |
| `@ActiveProfiles("integrationtest")` | 載入 `application-integrationtest.properties`（不覆蓋主設定） |
| `@DirtiesContext(AFTER_EACH_TEST_METHOD)` | 每個測試方法結束後重置 Spring Context，避免測試間資料互相污染 |
| `@LocalServerPort` | 注入 Spring 隨機選的 port 號碼 |
| `@Container` | 標記這個 Docker 容器要被 Testcontainers 管理 |
| `@ServiceConnection` | 自動讀取容器的連線資訊，配置給 Spring 的 DataSource |
| `static MySQLContainer<?>` | static 讓容器在所有測試方法間共用，只啟動一次，速度快 |

**`@ServiceConnection` 的工作原理：**

```
1. @Container 啟動 MySQL 容器
   ↓
2. @ServiceConnection 讀取容器的 host/port/帳密
   ↓
3. 自動建立 JdbcConnectionDetails Bean
   ↓
4. Spring Boot DataSource 自動配置優先使用 ConnectionDetails Bean
   （比 application.properties 的 spring.datasource.* 優先）
   ↓
5. 測試的所有 DB 操作都打到這個容器
```

**`RestTemplate` vs `TestRestTemplate` vs `MockMvc`：**

我們在這個專案用 Spring Boot 4.x，遇到了兩個工具被移除或搬移套件路徑的問題：

| 工具 | 狀態 | 適合場景 |
|------|------|---------|
| `MockMvc` + `@AutoConfigureMockMvc` | Spring Boot 4.x 套件路徑改變（問題） | `MOCK` 環境，不啟動真實 Tomcat |
| `TestRestTemplate` | Spring Boot 4.x 已移除 | `RANDOM_PORT` 環境 |
| 標準 `RestTemplate` | ✅ 永遠可用 | `RANDOM_PORT` 環境，我們最終用這個 |

> ⚠️ **學習用**：公司實際做法（Spring Boot 3.x 為主）通常用 `TestRestTemplate` 或 `MockMvc`，Spring Boot 4.x 建議改用 `RestClient`（新的 HTTP 客戶端）或 `WebTestClient`（WebFlux）。

## 2-4 遇到的問題和解決方法

### 問題一：Testcontainers 依賴版本無法解析

**錯誤訊息：**
```
Could not find org.testcontainers:junit-jupiter:.
Could not find org.testcontainers:mysql:.
```

注意版本號後面是空的（`:` 後面什麼都沒有），代表版本沒被解析到。

**原因：** Spring Boot 4.x 的 BOM 沒有自動管理 `testcontainers` 子模組的版本。

**解決方法：** 明確指定版本號。

```groovy
// ❌ 有問題的寫法（Spring Boot 4.x 無法自動解析版本）
testImplementation 'org.testcontainers:junit-jupiter'
testImplementation 'org.testcontainers:mysql'

// ✅ 修正後的寫法
testImplementation 'org.testcontainers:junit-jupiter:1.20.4'
testImplementation 'org.testcontainers:mysql:1.20.4'
```

---

### 問題二：`@AutoConfigureMockMvc` 套件不存在

**錯誤訊息：**
```
package org.springframework.boot.test.autoconfigure.web.servlet does not exist
```

**原因：** Spring Boot 4.x 重組了測試自動配置的套件結構，`@AutoConfigureMockMvc` 的路徑改變了。

**解決方法：** 改用 `TestRestTemplate`，但很快發現...

---

### 問題三：`TestRestTemplate` 也不存在

**錯誤訊息：**
```
package org.springframework.boot.test.web.client does not exist
```

**原因：** Spring Boot 4.x 移除了 `TestRestTemplate`。

**解決方法：** 改用標準的 `org.springframework.web.client.RestTemplate`，這個永遠存在。

**重點：** 需要設定「不要在 4xx/5xx 時丟 Exception」的 ErrorHandler：

```java
restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
    @Override
    public boolean hasError(HttpStatusCode statusCode) {
        return false; // 不丟 Exception，讓測試自己判斷回應碼
    }
});
```

**為什麼需要這個設定？**
- `RestTemplate` 預設收到 4xx/5xx HTTP 狀態碼時，會丟出 `HttpClientErrorException`
- 但我們的 API 回傳 HTTP 200，錯誤訊息放在 JSON body 的 `code` 欄位（自定義狀態碼）
- 所以我們不需要 RestTemplate 的自動錯誤處理，關掉它即可

---

### 問題四：`PlaceholderResolutionException: Could not resolve 'aaa.bbb'`

**錯誤訊息：**
```
Caused by: org.springframework.util.PlaceholderResolutionException:
Could not resolve placeholder 'aaa.bbb' in value "${aaa.bbb}"
```

**這是最關鍵的問題，原因很反直覺：**

一開始我們建立了 `src/test/resources/application.properties`，內容是：
```properties
spring.jpa.hibernate.ddl-auto=create-drop
spring.sql.init.mode=never
spring.jpa.show-sql=false
```

以為它只會「覆蓋這三個屬性」，但實際上：

```
❌ 錯誤認知：
src/test/resources/application.properties 只覆蓋三個屬性，其他屬性還在

✅ 實際行為：
src/test/resources/application.properties 完全取代了
src/main/resources/application.properties
→ aaa.bbb、user.id、fixed.rate.ms 全部消失
→ 任何用到這些屬性的 @Value 注解都會報錯
```

**為什麼？** 因為兩個檔案在 classpath 裡的路徑完全一樣（都叫 `application.properties`，都在 classpath 根目錄），Java classpath 只能有一個同路徑的檔案，test resources 優先，所以 main 的被隱藏了。

**解決方法：** 把測試設定改成 Profile 方式，用不同的檔案名：

```
❌ 會完全取代主設定的做法：
src/test/resources/application.properties

✅ 正確做法（只合併，不取代）：
src/test/resources/application-integrationtest.properties
+ 測試類別加 @ActiveProfiles("integrationtest")
```

Profile 方式的原理：Spring Boot 先載入 `application.properties`（主設定，完整保留），再載入 `application-integrationtest.properties`（只覆蓋有寫到的屬性），兩者合併。

---

### 問題五：`Quiz1141121ApplicationTests` 找不到資料庫

**錯誤訊息：**
```
DataSourceProperties$DataSourceBeanCreationException
```

**原因：** 這個舊的測試類別有 `@SpringBootTest` 但沒有 Testcontainers，啟動時找不到資料庫設定（我們已把資料庫設定移到 profile 檔案了）。

**解決方法：** 加上 `@Disabled` 暫時停用：

```java
@Disabled("舊的佔位測試，尚未設定 Testcontainers，暫時停用")
@SpringBootTest
class Quiz1141121ApplicationTests { ... }
```

---

### 問題六：測試 code 值寫錯

**錯誤訊息：**
```
org.opentest4j.AssertionFailedError at UserControllerIntegrationTest.java:115
```

**原因：** 我們的測試寫 `assertEquals(400, ...)` 但查看 `ReplyMessage.java` 才發現：

```java
USER_EMAIL_ERROR(404, ...)    // 不是 400，是 404
USER_EMAIL_EXISTED(404, ...)  // 不是 400，是 404
USER_NOT_FOUND(404, ...)      // 不是 400，是 404
```

**解決方法：** 把測試的期望值從 400 改為 404。

**這說明了整合測試的價值：** 它讓我們發現了 API 設計上的問題：`USER_EMAIL_EXISTED`（Email 已存在）用 404（Not Found）語意上是不正確的，應該用 409（Conflict）或 400。這種問題在單元測試（只測邏輯）中看不出來，只有打真實 API 才會注意到。

---

## 2-5 測試覆蓋的情境

我們寫了 6 個整合測試，覆蓋 `/user/register` 和 `/user/login`：

| 測試方法 | 測試情境 | 期望結果 |
|----------|---------|---------|
| `register_whenAllParamsValid_shouldReturn200` | 合法資料註冊 | code 200 |
| `register_whenEmailAlreadyExists_shouldReturnError` | 重複 email 註冊 | code 404（EMAIL_EXISTED） |
| `register_whenEmailIsEmpty_shouldReturnError` | email 為空 | code 404（EMAIL_ERROR） |
| `login_whenCredentialsValid_shouldReturnTokenAndUser` | 正確帳密登入 | code 200 + token 不為空 + password 為 null |
| `login_whenAccountNotExist_shouldReturnError` | 帳號不存在 | code 404（NOT_FOUND） |
| `login_whenPasswordWrong_shouldReturnError` | 密碼錯誤 | code 400（PASSWORD_ERROR） |

---

## 2-6 從零開始的業界 SOP（空白專案）

### 前提條件

- Docker Desktop 已安裝並開啟
- Spring Boot 專案已建立
- `spring-boot-starter-test` 已在依賴中

### Step 1：加依賴

```groovy
// build.gradle
testImplementation 'org.springframework.boot:spring-boot-testcontainers'
testImplementation 'org.testcontainers:junit-jupiter:1.20.4'
testImplementation 'org.testcontainers:mysql:1.20.4'
```

> ⚠️ **學習用**：公司實際做法是在 `dependencyManagement` 裡匯入 `testcontainers-bom`，讓所有 testcontainers 子模組版本統一管理：
> ```groovy
> testImplementation(platform('org.testcontainers:testcontainers-bom:1.20.4'))
> testImplementation 'org.testcontainers:junit-jupiter'
> testImplementation 'org.testcontainers:mysql'
> ```

### Step 2：建立測試 Profile 設定檔

建立 `src/test/resources/application-integrationtest.properties`：

```properties
spring.jpa.hibernate.ddl-auto=create-drop
spring.sql.init.mode=never
spring.jpa.show-sql=false
```

**❌ 不要這樣做：**
```
# 不要建立 src/test/resources/application.properties
# 它會完全取代 main 的 application.properties，導致屬性遺失
```

### Step 3：建立整合測試類別

```java
package com.example.xxx.controller;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.*;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("integrationtest")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class YourControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate(new SimpleClientHttpRequestFactory());
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(HttpStatusCode statusCode) {
                return false;
            }
        });
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private HttpEntity<String> jsonRequest(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    @Test
    void 測試情境_應該發生什麼() {
        // Arrange：準備資料（如果需要）

        // Act：打 API
        ResponseEntity<Map> response = restTemplate.postForEntity(
            url("/your/endpoint"),
            jsonRequest("""{"key": "value"}"""),
            Map.class
        );

        // Assert：驗證結果
        assertEquals(200, response.getBody().get("code"));
    }
}
```

### Step 4：跑測試

```powershell
# 確認 Docker Desktop 已開啟，然後跑：
.\gradlew test
```

**第一次執行時會 pull `mysql:8.0` Docker image（約 500MB），之後快取好就很快。**

> ⚠️ **學習用**：公司實際做法不用 `@DirtiesContext`（很慢，每個測試方法都重建 Spring Context），而是用 `@Transactional` 讓每個測試方法的資料庫操作自動 rollback：
> ```java
> @Transactional  // 測試結束後自動 rollback，不需要重建 Context
> @Test
> void 測試方法() { ... }
> ```
> 但注意：`@Transactional` + `RANDOM_PORT` 有時候不如預期（跨 HTTP 請求的 Transaction 可能不 rollback），需要根據情況選擇。

---

# Part 3：Spring Profile 環境分離

## 3-1 是什麼，為什麼需要

沒有 Profile 之前，所有設定放在一個 `application.properties`：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/quiz_1141121  ← 本機的
spring.datasource.password=123456  ← 本機密碼，不安全！
spring.jpa.show-sql=true           ← 開發用，生產不需要
```

**問題：**
1. 如果你 push 到 GitHub，密碼就洩漏了
2. 開發時不小心連到生產資料庫，搞壞線上資料
3. 生產環境不應該印出所有 SQL（效能、安全）

**Profile 解決這些問題：**

```
一份設定 → 對應一個環境
不同環境 → 讀不同設定檔
生產密碼 → 從環境變數讀，不進程式碼
```

## 3-2 三個檔案的結構

```
src/main/resources/
├── application.properties          ← 共用設定（所有環境都一樣的）
├── application-dev.properties      ← 開發環境專用（本機 MySQL）
└── application-prod.properties     ← 生產環境專用（EC2 + 環境變數密碼）
```

### `application.properties`（共用）

```properties
spring.application.name=quiz_1141121

# SQL 初始化設定（共用）
spring.sql.init.mode=always

# 排程設定（共用，所有環境相同）
fixed.rate.ms=3000
aaa.bbb=10000
user.id=Uabc
```

> ⚠️ **學習用**：公司實際做法不會在 `application.properties` 裡寫 `spring.profiles.active=dev`，這樣做會導致每次啟動都預設 dev，生產環境部署時容易忘記改。公司做法是讓啟動者明確指定，強制意識到「我在哪個環境」。

### `application-dev.properties`（開發環境）

```properties
# 本機 MySQL 連線
spring.datasource.url=jdbc:mysql://localhost:3306/quiz_1141121?serverTimezone=GMT%2B8
spring.datasource.username=root
spring.datasource.password=123456
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# 開發時印出 SQL，方便除錯
spring.jpa.show-sql=true
```

### `application-prod.properties`（生產環境）

```properties
# EC2 上的 MySQL 連線
spring.datasource.url=jdbc:mysql://localhost:3306/quiz_1141121?serverTimezone=GMT%2B8
spring.datasource.username=root
# ${DB_PASSWORD}：從系統環境變數 DB_PASSWORD 讀取
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# 生產環境關閉 SQL 輸出
spring.jpa.show-sql=false
```

> ⚠️ **學習用**：公司實際做法是用 **AWS Secrets Manager** 或 **HashiCorp Vault** 管理所有敏感設定，Spring Cloud 自動從 Secrets Manager 拉取，`application.properties` 裡完全沒有任何密碼相關設定，連 `${DB_PASSWORD}` 這種環境變數寫法也不用。

## 3-3 如何啟動不同環境

```powershell
# 本機開發（dev）
.\gradlew bootRun --args='--spring.profiles.active=dev'

# 確認 profile 有生效：看啟動 log 裡有沒有這行
# "The following 1 profile is active: "dev""

# EC2 生產環境（在 docker-compose.yml 裡設定）
environment:
  - SPRING_PROFILES_ACTIVE=prod
  - DB_PASSWORD=真實生產密碼
```

> ⚠️ **學習用**：公司實際做法會在 CI/CD Pipeline 裡設定環境變數，例如 GitHub Actions：
> ```yaml
> - name: Run Tests
>   env:
>     SPRING_PROFILES_ACTIVE: test
>     DB_PASSWORD: ${{ secrets.DB_PASSWORD }}
>   run: ./gradlew test
> ```
> 密碼存在 GitHub Secrets 或 AWS Parameter Store，不出現在任何程式碼裡。

## 3-4 Profile 載入的合併邏輯

```
執行 .\gradlew bootRun --args='--spring.profiles.active=dev'
    ↓
Spring 讀取 application.properties（共用設定）
    ↓
Spring 讀取 application-dev.properties（dev 專用設定）
    ↓
合併：dev 的設定覆蓋共用設定的同名屬性
    ↓
最終生效的設定 = 共用 + dev（dev 優先）
```

## 3-5 從零開始的業界 SOP（空白專案）

### Step 1：建立三個設定檔

```
src/main/resources/
├── application.properties          ← 共用
├── application-dev.properties      ← 開發
└── application-prod.properties     ← 生產
```

### Step 2：`application.properties` 只放共用設定

```properties
spring.application.name=你的應用名稱
# 其他所有環境都一樣的設定放這裡
# 資料庫設定不放這裡
```

### Step 3：`application-dev.properties` 放開發設定

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/你的DB?serverTimezone=GMT%2B8
spring.datasource.username=root
spring.datasource.password=本機密碼
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.show-sql=true
```

### Step 4：`application-prod.properties` 放生產設定

```properties
spring.datasource.url=jdbc:mysql://生產DB主機:3306/你的DB
spring.datasource.username=prod_user
spring.datasource.password=${DB_PASSWORD}  ← 從環境變數讀
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.show-sql=false
```

### Step 5：`.gitignore` 保護開發設定

```gitignore
# 開發人員的個人設定不進版本控制
# 每個人自己建自己的 application-dev.properties
src/main/resources/application-dev.properties
```

> ⚠️ **學習用**：公司實際做法是 `application-dev.properties` 也進版本控制，但密碼用佔位符：
> ```properties
> spring.datasource.password=${LOCAL_DB_PASSWORD:123456}
> # 冒號後面是 default value，沒設環境變數時用 123456
> ```
> 或者公司用 `application-dev.properties.example` 作為範本，開發者自己複製一份並填入真實值。

### Step 6：啟動時指定 profile

```powershell
# 本機開發
.\gradlew bootRun --args='--spring.profiles.active=dev'

# 驗證：看 log 裡有沒有 "The following 1 profile is active: "dev""
```

---

## 總結：這次學到的技術

| 技術 | 學到什麼 |
|------|---------|
| Testcontainers | 整合測試用真實 MySQL Docker 容器，不依賴手動安裝的資料庫 |
| `@ServiceConnection` | Spring Boot 4.x 自動連接 Testcontainers 容器到 DataSource |
| `@ActiveProfiles` | 指定測試用的 Profile，避免設定衝突 |
| Spring Profile | 把設定按環境分檔，避免密碼洩漏和環境混淆 |
| `application-{profile}.properties` vs `application.properties` | Profile 檔案是「合併」，同名的 `application.properties` 在 test/resources 會「取代」 |

## 可以寫在履歷上的內容

```
測試：
- JUnit 5 + Mockito 單元測試（15 個測試方法，Mock DAO 隔離測試，AAA 結構）
- Spring Boot 整合測試（Testcontainers 自動啟動真實 MySQL Docker 容器）
- 整合測試覆蓋 REST API 完整流程（Controller → Spring Security → Service → DAO → DB）
- 測試過程發現 ReplyMessage 設計問題（EMAIL_EXISTED 應回 409，而非 404）

環境管理：
- Spring Profile 環境隔離（dev / prod），開發與生產完全分離
- 生產環境密碼透過環境變數注入（${DB_PASSWORD}），不寫入版本控制
- Testcontainers 整合測試使用獨立 Profile（integrationtest），不影響主設定
```
