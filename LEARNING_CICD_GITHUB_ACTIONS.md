# CI/CD + GitHub Actions + Testcontainers 學習手冊

> 目標：每次 push 程式碼，GitHub 自動跑測試，測試過了才部署。
> 開發者不需要手動啟動 Docker Desktop，CI 的 Ubuntu runner 內建 Docker。

---

## Part 1：核心概念

### 什麼是 CI/CD？

```
CI = Continuous Integration（持續整合）
CD = Continuous Delivery / Deployment（持續交付 / 部署）
```

**CI 做什麼：**
每次有人 push 程式碼，自動：
1. 把程式碼下載到一台乾淨的虛擬機
2. 跑所有測試
3. 報告結果（成功 ✅ 或失敗 ❌）

**CD 做什麼：**
CI 通過後，自動：
1. 打包成 Docker image
2. 推到 Docker Hub
3. 部署到雲端伺服器（EC2）

**效果：**
- 你 push 程式碼
- 5 分鐘後收到通知：測試全過了，已自動部署到生產環境
- 或：測試失敗，部署被擋住，需要修好再 push

---

### 什麼是 GitHub Actions？

GitHub 內建的 CI/CD 工具。免費配額：
- Public repo：無限
- Private repo：每月 2,000 分鐘（個人方案）

**設定方式：** 在 `.github/workflows/` 放 `.yml` 設定檔

**關鍵詞對照表：**

| 術語 | 意思 |
|------|------|
| `workflow` | 一個 `.yml` 檔案，定義整個 CI/CD 流程 |
| `trigger (on:)` | 什麼事件觸發這個 workflow（push、PR） |
| `job` | 流程中的一個工作單位（跑測試、build Docker） |
| `step` | job 裡面的每一個步驟 |
| `runner` | 執行 job 的虛擬機（`ubuntu-latest` = GitHub 提供的 Ubuntu） |
| `action` | 別人寫好的可重用步驟（`uses: actions/checkout@v4`） |
| `secret` | 加密的環境變數（DB密碼、Docker Hub token 等） |
| `artifact` | job 執行結果產生的檔案（測試報告） |

---

### 為什麼 Testcontainers 在 CI 能直接用？

```
本機開發時：
你的電腦 → 需要手動開 Docker Desktop → Testcontainers 找到 Docker → 啟動 MySQL 容器

GitHub Actions CI 時：
ubuntu-latest runner → Docker daemon 已內建，自動啟動 → Testcontainers 找到 Docker → 啟動 MySQL 容器
```

**關鍵：** `ubuntu-latest` 這台虛擬機預裝了 Docker daemon，Testcontainers 會自動偵測並使用它。
你的 `ci.yml` 不需要任何額外設定，只要跑 `./gradlew test` 就能執行整合測試。

---

## Part 2：這個專案的 CI/CD 架構

```
.github/workflows/ci.yml
```

### 觸發條件

```yaml
on:
  push:
    branches: [ main, master ]    # push 到主分支：跑測試 → 部署
  pull_request:
    branches: [ main, master ]    # PR：只跑測試（不部署）
```

### 兩個 Job 的關係

```
push 到 master
      │
      ▼
┌─────────────────────────────┐
│  Job 1: test                │  ← 跑 ./gradlew test
│  單元測試 + 整合測試         │    （Testcontainers 自動啟動 MySQL）
└──────────────┬──────────────┘
               │ 通過才繼續
               ▼
┌─────────────────────────────┐
│  Job 2: build-and-push      │  ← Build JAR → 推 Docker image
│  只在 push 時執行            │
└─────────────────────────────┘
               │
               ▼
         Docker Hub
```

**`needs: test`** → Job 2 必須等 Job 1 通過，測試失敗就不會部署。
**`if: github.event_name == 'push'`** → PR 時不推 image（因為程式碼還沒合進 master）。

---

### 設定檔逐行解釋

```yaml
name: CI Pipeline

on:
  push:
    branches: [ main, master ]
  pull_request:
    branches: [ main, master ]

jobs:

  test:
    name: Run Unit & Integration Tests
    runs-on: ubuntu-latest        # 使用 GitHub 提供的 Ubuntu 虛擬機

    steps:
      - name: Checkout source code
        uses: actions/checkout@v4  # 把你的 repo 程式碼下載到虛擬機

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'  # Eclipse Temurin，免費 JDK 發行版
          cache: gradle             # 自動 cache Gradle 依賴，第二次更快

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew      # Linux 需要手動給執行權限（Windows 不用）

      - name: Run all tests
        run: ./gradlew test        # 跑所有測試，包含 Testcontainers 整合測試

      - name: Upload test report
        uses: actions/upload-artifact@v4
        if: always()               # 測試失敗也要上傳報告（才能知道哪裡壞了）
        with:
          name: test-report
          path: build/reports/tests/test/
          retention-days: 7        # 報告保留 7 天

  build-and-push:
    name: Build and Push Docker Image
    runs-on: ubuntu-latest
    needs: test                    # 等 test job 完成且通過
    if: github.event_name == 'push' # PR 不執行這個 job

    steps:
      # ... checkout, setup-java, chmod +x（同上）...
      
      - name: Build JAR
        run: ./gradlew build -x test  # 測試已在 test job 跑過，這裡跳過

      - name: Log in to Docker Hub
        uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKERHUB_USERNAME }}  # 從 repo Secrets 讀取
          password: ${{ secrets.DOCKERHUB_TOKEN }}

      - name: Build and Push Docker image
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: ${{ secrets.DOCKERHUB_USERNAME }}/quiz-app:latest
```

---

## Part 3：使用場景

### 場景一：個人開發（單人專案）

**日常開發流程：**

```
1. 本機寫程式碼（.\gradlew bootRun --args='--spring.profiles.active=dev'）
2. 本機跑測試確認沒壞（.\gradlew test）
3. git add / git commit / git push
4. GitHub Actions 自動觸發
5. 去 GitHub repo → Actions 頁面看結果
6. 全綠 ✅ → Docker image 自動推到 Docker Hub
7. 失敗 ❌ → 點進去看是哪個 step 失敗，修好再 push
```

**在哪裡看 CI 結果：**
```
GitHub repo 頁面
  → 上方 "Actions" 頁籤
  → 左側選 "CI Pipeline"
  → 點進最新的執行記錄
  → 看每個 job 和 step 的 log
```

**下載測試報告：**
```
Actions 執行記錄頁面
  → 下方 "Artifacts" 區塊
  → 下載 "test-report"
  → 解壓後用瀏覽器打開 index.html
```

---

### 場景二：團隊合作（多人專案）

**標準 Git Flow：**

```
main（保護分支，不能直接 push）
  ↑
feature/add-login
feature/fix-quiz-bug
feature/rbac
```

**開發一個新功能的完整流程：**

```
1. 從 main 建立 feature branch
   git checkout -b feature/add-quiz-search

2. 寫程式碼 + 寫測試

3. push 到 GitHub
   git push origin feature/add-quiz-search

4. 在 GitHub 建立 PR（Pull Request）
   → PR 描述這個 branch 做了什麼
   → 觸發 CI：只跑測試（不部署）

5. CI 結果顯示在 PR 頁面
   → 全過 ✅：可以繼續 Code Review
   → 失敗 ❌：PR 被擋住，作者要修好才能合併

6. 團隊成員 Code Review
   → 提意見、討論、修改

7. Review 通過 + CI 全過 → 合併進 main
   → 觸發 CI：跑測試 → 測試過了 → 部署到生產

8. 自動部署完成，功能上線
```

**Branch Protection Rules（設定方式）：**
```
GitHub repo 頁面
  → Settings → Branches → Add rule
  → Branch name pattern: main
  → 勾選：
    ✅ Require status checks to pass before merging
    ✅ Require branches to be up to date before merging
    ✅ Status checks: 選 "Run Unit & Integration Tests"（你的 job name）
```

效果：CI 沒過 → 合併按鈕變灰色，強制不能合併。

---

### 場景三：緊急修 Bug（Hotfix）

```
1. 從 main 建立 hotfix branch
   git checkout -b hotfix/fix-login-crash

2. 修 bug

3. push + PR → CI 跑測試確認修好了

4. 通過後合併進 main，自動部署
```

---

## Part 4：在舊專案上的 SOP（本專案）

### 前置條件確認

- [ ] 專案已推到 GitHub（有 remote repo）
- [ ] Docker Desktop 有開（本機跑測試用）
- [ ] `.\gradlew test` 本機全過
- [ ] Docker Hub 帳號已建立

### 步驟一：確認 CI 設定檔已就位

本專案已有：`.github/workflows/ci.yml`

### 步驟二：在 GitHub 設定 Docker Hub Secrets

```
GitHub repo 頁面
  → Settings
  → Secrets and variables
  → Actions
  → New repository secret

加入：
  DOCKERHUB_USERNAME = 你的 Docker Hub 帳號名
  DOCKERHUB_TOKEN    = Docker Hub 的 Access Token（不是密碼）
```

**如何取得 Docker Hub Access Token：**
```
登入 hub.docker.com
  → 右上角帳號 → Account Settings
  → Security → New Access Token
  → 命名（例如 github-actions）→ Generate
  → 複製 token（只顯示一次）
```

### 步驟三：push 到 GitHub 觸發 CI

```powershell
git add .github/workflows/ci.yml
git commit -m "ci: add automated test and deploy workflow"
git push origin master
```

### 步驟四：在 GitHub Actions 確認結果

```
GitHub repo → Actions 頁籤
  → 看到 "CI Pipeline" 正在跑
  → 等待 Job 1 "Run Unit & Integration Tests" 完成
  → 通過後看 Job 2 "Build and Push Docker Image"
```

### 步驟五：確認 Docker Hub 有新 image

```
登入 hub.docker.com
  → Repositories
  → 看到 quiz-app:latest 更新時間是剛才
```

---

## Part 5：空白專案 SOP（從零到 CI/CD 可運行）

### 1. 建立 Spring Boot 專案

```
https://start.spring.io
  → Gradle - Groovy
  → Java 17
  → Dependencies: Spring Web, Spring Data JPA, MySQL Driver, Validation, Spring Security
  → Generate → 下載 → 解壓
```

### 2. 本機開發設定

**`src/main/resources/application.properties`（共用設定）：**
```properties
spring.application.name=your-app
spring.sql.init.mode=always
```

**`src/main/resources/application-dev.properties`：**
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/your_db?serverTimezone=GMT%2B8&useSSL=false
spring.datasource.username=root
spring.datasource.password=你的本機密碼
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.show-sql=true
spring.jpa.hibernate.ddl-auto=update
```

**`src/main/resources/application-prod.properties`：**
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/your_db?serverTimezone=GMT%2B8&useSSL=false
spring.datasource.username=root
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.show-sql=false
spring.jpa.hibernate.ddl-auto=none
```

### 3. 加入 Testcontainers 依賴

**`build.gradle`：**
```groovy
dependencies {
    // ... 其他依賴 ...

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.boot:spring-boot-testcontainers'
    testImplementation 'org.testcontainers:junit-jupiter:1.20.4'
    testImplementation 'org.testcontainers:mysql:1.20.4'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}
```

> ⚠️ **注意**：Spring Boot 4.x 的 BOM 不會自動解析 testcontainers 子模組版本，必須手動加 `:1.20.4`。

### 4. 建立整合測試 profile 設定

**`src/test/resources/application-integrationtest.properties`：**
```properties
spring.jpa.hibernate.ddl-auto=create-drop
spring.sql.init.mode=never
spring.jpa.show-sql=false
```

> ⚠️ **重要**：不要用 `src/test/resources/application.properties`，那個檔案會完全覆蓋 main 的 `application.properties`，導致 `aaa.bbb` 這類共用設定消失。用 profile 命名的檔案才是合併（merge）而非覆蓋。

### 5. 撰寫整合測試

```java
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
            public boolean hasError(org.springframework.http.HttpStatusCode statusCode) {
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
    void someEndpoint_whenValid_shouldReturn200() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
            url("/your/endpoint"),
            jsonRequest("""{ "field": "value" }"""),
            Map.class
        );
        assertEquals(200, response.getBody().get("code"));
    }
}
```

### 6. 建立 Dockerfile

**`Dockerfile`：**
```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 7. 建立 GitHub Actions CI/CD

**`.github/workflows/ci.yml`：**
```yaml
name: CI Pipeline

on:
  push:
    branches: [ main, master ]
  pull_request:
    branches: [ main, master ]

jobs:
  test:
    name: Run Unit & Integration Tests
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: gradle
      - run: chmod +x gradlew
      - run: ./gradlew test
      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: test-report
          path: build/reports/tests/test/
          retention-days: 7

  build-and-push:
    name: Build and Push Docker Image
    runs-on: ubuntu-latest
    needs: test
    if: github.event_name == 'push'
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: gradle
      - run: chmod +x gradlew
      - run: ./gradlew build -x test
      - uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKERHUB_USERNAME }}
          password: ${{ secrets.DOCKERHUB_TOKEN }}
      - uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: ${{ secrets.DOCKERHUB_USERNAME }}/your-app:latest
```

### 8. 在 GitHub 設定 Secrets

```
repo → Settings → Secrets and variables → Actions → New repository secret

DOCKERHUB_USERNAME = 你的 Docker Hub 帳號
DOCKERHUB_TOKEN    = Docker Hub 的 Access Token
```

### 9. 推上 GitHub，確認 CI 跑起來

```bash
git add .
git commit -m "ci: setup GitHub Actions with Testcontainers integration test"
git push origin main
```

去 GitHub → Actions 頁面確認全綠。

---

## Part 6：常見錯誤與解法

### 錯誤 1：Testcontainers 找不到 Docker

```
Could not connect to Docker
```

**原因：** 本機沒開 Docker Desktop，或 CI runner 沒有 Docker。

**解法：**
- 本機：開啟 Docker Desktop
- CI：確認用 `ubuntu-latest`（不是 `windows-latest`，Windows runner 沒 Docker）

---

### 錯誤 2：測試 profile 找不到資料庫設定

```
DataSourceBeanCreationException: Failed to determine suitable datasource
```

**原因：** 沒有加 `@ServiceConnection`，或 `@Container` 沒有 `static`。

**解法：**
```java
@Container
@ServiceConnection
static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");
// ↑ 必須是 static，否則每個 @Test 都建新容器，慢而且可能出錯
```

---

### 錯誤 3：PlaceholderResolutionException

```
Could not resolve placeholder 'aaa.bbb' in value "${aaa.bbb}"
```

**原因：** 在 `src/test/resources/application.properties` 放了設定，把 main 的 `application.properties` 完全覆蓋掉了。

**解法：** 刪掉 `src/test/resources/application.properties`，改用 `src/test/resources/application-integrationtest.properties`，測試類別加 `@ActiveProfiles("integrationtest")`。

---

### 錯誤 4：CI 測試通過但 Docker 推送失敗

```
Error: Username and password required
```

**原因：** GitHub Secrets 沒設定，或名稱拼錯。

**解法：** 確認 repo → Settings → Secrets 有 `DOCKERHUB_USERNAME` 和 `DOCKERHUB_TOKEN`，且名稱完全相同（區分大小寫）。

---

### 錯誤 5：gradlew Permission Denied

```
Permission denied: ./gradlew
```

**原因：** Linux 環境下 gradlew 沒有執行權限。

**解法：** 在 CI 步驟中加 `run: chmod +x gradlew`。

---

## Part 7：公司實際做法 vs 學習做法對照

| 項目 | 本專案學習做法 | 公司實際做法 |
|------|--------------|------------|
| 觸發條件 | push/PR 到 main/master | 通常還有 `workflow_dispatch`（手動觸發）、`schedule`（定時跑） |
| Secrets 管理 | GitHub Secrets | HashiCorp Vault / AWS Secrets Manager |
| 測試報告 | Artifact 下載 | SonarQube / 整合進 PR Comment |
| 部署方式 | push 到 Docker Hub | push 後觸發 ArgoCD（GitOps）或 AWS CodeDeploy |
| Branch 策略 | 直接 push master | 嚴格 Git Flow：main / develop / feature / hotfix |
| Code Review | 沒有強制 | PR 必須 1-2 人 approve 才能合併 |
| 測試覆蓋率 | 沒有強制 | Jacoco 報告，低於 80% CI 自動失敗 |

---

## Part 8：履歷上可以寫的內容

```
技術：GitHub Actions · Docker · Testcontainers · Spring Boot · MySQL

成果：
- 建立 CI/CD Pipeline，每次 push 自動執行單元測試與整合測試
- 使用 Testcontainers 在 CI 環境啟動真實 MySQL 容器進行整合測試，
  確保測試環境與生產環境一致，杜絕 Mock 資料庫遮蔽真實 SQL 錯誤
- 實作 test → build-and-push 的 Job 依賴關係，
  測試未通過則自動阻斷部署流程
- 導入 Spring Profile 環境分離（dev / prod），
  生產環境密碼透過環境變數注入，不進入 git 版本控制
```
