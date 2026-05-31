# 📋 Dynamic Questionnaire System

> 全端動態問卷系統 — **Spring Boot 4 後端 API × Angular 19 前端**，部署於 AWS EC2，支援手機端操作。

<div align="center">

[![Live Demo](https://img.shields.io/badge/🚀_Live_Demo-點我體驗-success?style=for-the-badge)](https://beesuperman.github.io/dynamic-questionnaire-frontend/)
[![Swagger UI](https://img.shields.io/badge/📖_Swagger_UI-API文件-blue?style=for-the-badge)](http://3.105.140.44:8080/swagger-ui.html)

</div>

<div align="center">

[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.2-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/17/)
[![Angular](https://img.shields.io/badge/Angular-19-DD0031?style=for-the-badge&logo=angular&logoColor=white)](https://angular.io/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-7.2-DC382D?style=for-the-badge&logo=redis&logoColor=white)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)
[![AWS EC2](https://img.shields.io/badge/AWS-EC2-FF9900?style=for-the-badge&logo=amazonaws&logoColor=white)](https://aws.amazon.com/ec2/)

</div>

---

## 🚀 立即體驗（無需安裝）

| 入口 | 網址 | 說明 |
|------|------|------|
| **前端 Demo** | [beesuperman.github.io/dynamic-questionnaire-frontend](https://beesuperman.github.io/dynamic-questionnaire-frontend/) | 可一鍵體驗，無需帳號 |
| **Swagger UI** | [3.105.140.44:8080/swagger-ui.html](http://3.105.140.44:8080/swagger-ui.html) | 後端 API 文件，可直接測試 |

### Demo 帳號
| 角色 | Email | 密碼 |
|------|-------|------|
| 管理員 | admin222@gmail.com | 12345678 |
| 一般使用者 | 111@gmail.com | 12345678 |

> 登入頁也提供「一鍵體驗」按鈕，可不使用後端直接瀏覽 Mock 資料。

---

## 📌 專案簡介

本專案是完整的全端動態問卷系統，展示從需求分析、設計、開發、測試到部署的完整流程：

**後端（本 Repo）** — Spring Boot 4 RESTful API
- 三層架構、JWT 認證、Redis 快取、全域例外處理
- JUnit 5 + Mockito 單元測試、Testcontainers 整合測試
- SLF4J + MDC Correlation ID 請求追蹤日誌
- Docker Compose 容器化、GitHub Actions CI/CD 自動部署至 AWS EC2

**前端** — Angular 19 SPA（[前端 Repo](https://github.com/BeeSuperman/dynamic-questionnaire-frontend)）
- 管理員後台：問卷 CRUD、統計圖表
- 使用者前台：填寫問卷、查看結果
- RWD 響應式設計，支援手機端操作

---

## 👥 使用者角色

| 角色 | 登入需求 | 可執行操作 |
|------|---------|-----------|
| **管理員** | 需登入（admin 開頭 Email） | 新增 / 編輯 / 刪除問卷與題目、查看所有作答記錄與統計圖表 |
| **會員** | 需登入 | 填寫問卷、查詢自己的歷史作答記錄 |
| **訪客** | 無需帳號 | 直接填寫問卷（自動建立臨時帳號），降低使用門檻 |

題目支援**單選、多選、文字填答**三種形式，問卷結構可透過管理後台隨時調整，無需修改程式碼。

---

## 🏗️ 系統架構

```
┌─────────────────────────────────────────────────────┐
│                   AWS EC2 (t3.micro)                │
│                                                     │
│  ┌─────────────────┐    ┌────────────────────────┐  │
│  │  Spring Boot 4  │    │       MySQL 8.0        │  │
│  │   Port: 8080    │◄──►│      Port: 3306        │  │
│  │                 │    └────────────────────────┘  │
│  │  JwtAuthFilter  │    ┌────────────────────────┐  │
│  │  LoggingFilter  │◄──►│       Redis 7.2        │  │
│  │  GlobalException│    │      Port: 6379        │  │
│  └─────────────────┘    └────────────────────────┘  │
└─────────────────────────────────────────────────────┘
         ▲ HTTP / ngrok HTTPS
         │
┌─────────────────────────────┐
│  Angular 19 (GitHub Pages)  │
│  beesuperman.github.io      │
└─────────────────────────────┘
```

**請求流程：** 前端 → JwtAuthFilter（JWT 驗證）→ LoggingFilter（MDC Correlation ID）→ Controller → Service → DAO → MySQL / Redis

---

## 🔧 技術棧

### 後端
| 類別 | 技術 | 版本 |
|------|------|------|
| 框架 | Spring Boot | 4.0.2 |
| 語言 | Java | 17 |
| ORM | Spring Data JPA | 4.x |
| 資料庫 | MySQL | 8.0 |
| 快取 | Redis + Spring Cache（`@Cacheable`） | 7.2 |
| 安全性 | Spring Security + JWT + BCrypt | 6.x |
| 單元測試 | JUnit 5 + Mockito | 5.x |
| 整合測試 | Testcontainers（真實 MySQL + Redis 容器） | 1.x |
| 日誌 | SLF4J + Logback + MDC Correlation ID | 內建 |
| API 文件 | Springdoc OpenAPI（Swagger UI） | 3.x |
| 容器化 | Docker + Docker Compose | - |
| CI/CD | GitHub Actions（測試 → Build → Deploy） | - |
| 雲端部署 | AWS EC2（t3.micro, Sydney） | - |

### 前端
| 類別 | 技術 |
|------|------|
| 框架 | Angular 19（Standalone Component） |
| 樣式 | SCSS + RWD Media Queries |
| HTTP | Angular HttpClient + 功能型 Interceptor |
| 圖表 | Chart.js |
| 部署 | GitHub Pages（`ng build` + `gh-pages`） |

---

## ✨ 技術亮點

### 1. JWT 認證機制
登入成功後後端產生 JWT Token，前端透過 Angular **HttpInterceptor** 自動在所有請求 Header 帶上 `Authorization: Bearer <token>`，後端的 `JwtAuthFilter` 逐層驗證：

```java
// JwtAuthFilter.java — 每個請求進來都執行
String token = request.getHeader("Authorization").substring(7);
String email = jwtUtil.validateToken(token);
// 驗證通過 → 注入 SecurityContext，後續 Controller 可取得當前使用者
```

### 2. Redis 分散式快取
使用 Spring Cache 抽象層搭配 Redis，對高頻查詢加速，並在資料異動時精準清除對應快取：

```java
@Cacheable(value = "quizList", key = "'all'")   // 查詢時自動快取
public GetQuizRes getQuizList() { ... }

@CacheEvict(value = "quizList", allEntries = true)  // 新增/更新/刪除後清除
public CreateRes create(CreateReq req) { ... }
```

### 3.測試策略： 
以 Mockito 撰寫 Service 層單元測試；以 Testcontainers 啟動真實 MySQL 與 Redis 容器進行 Controller 整合測試，確保本地與 CI 環境行為完全一致，CI/CD 無需額外設定基礎設施服務：

```java
@Testcontainers
class QuizServiceIntegrationTest {
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.2-alpine");
}
```

### 4. MDC Correlation ID 請求追蹤
每個 HTTP 請求進入時，`LoggingFilter` 產生唯一 `traceId` 注入 SLF4J MDC，讓同一個請求的所有 log 可以被串連追蹤：

```java
// LoggingFilter.java
String traceId = UUID.randomUUID().toString().substring(0, 8);
MDC.put("traceId", traceId);
// 所有 log 自動帶上 [traceId] 前綴
log.info("{} {} → {} ({}ms)", method, uri, status, duration);
```

### 5. GitHub Actions CI/CD Pipeline
Push 到 `master` 後自動觸發三階段 Pipeline，**總耗時約 2.5 分鐘**，零手動介入完成持續部署：
整合 GitHub Actions 建立三階段自動化 Pipeline，push 到 master 後依序執行測試、建置 Docker image 推送至 Docker Hub，並透過 SSH 自動部署至 AWS EC2（Ubuntu），實現零手動介入的持續部署流程，總耗時約 2.5 分鐘。部署端以 ngrok 建立永久 HTTPS Tunnel 並設定 systemd 服務開機自動啟動，解決 GitHub Pages（HTTPS）無法呼叫裸 HTTP API 的混合內容限制問題。
```
push to master
      │
      ▼
┌─────────────┐     ┌──────────────────┐     ┌─────────────────────┐
│  1. test    │────▶│   2. build       │────▶│   3. deploy         │
│             │     │                  │     │                     │
│ JUnit 5     │     │ docker build     │     │ SSH into EC2        │
│ Mockito     │     │ push to          │     │ docker compose pull │
│ Testcontain │     │ Docker Hub       │     │ docker compose up   │
└─────────────┘     └──────────────────┘     └─────────────────────┘
  ~40s                  ~60s                      ~30s
```

前兩個 Job 失敗時自動中止，不會部署有問題的版本。

### 6. 資料一致性：智慧差異更新（Diff Update Strategy）
更新問卷題目時，**不刪除所有題目再重建**（避免作答記錄外鍵關聯遺失），而是透過 Stream API 比對新舊 ID，精準執行「保留 / 新增 / 刪除」：

```java
List<Integer> idsToDelete = oldIds.stream()
    .filter(id -> !newIds.contains(id)).toList();
if (!idsToDelete.isEmpty()) {
    questionDao.deleteByIds(req.getQuizId(), idsToDelete);
}
```

### 7. RWD 響應式設計（前端）
針對手機版修正多項問題：
- Angular 自訂元件預設 `display: inline` 導致寬度壓縮 → 全域設定 `display: block`
- 固定定位導覽列與頁面內容重疊 → 各頁面加上對應 `margin-top`
- 表格在手機上欄位過窄 → 加上 `overflow-x: auto` + `min-width` 支援橫向滑動
- Modal 寬度固定 → 改為 `95vw` 自適應

### 8.架構設計：
嚴格遵循三層式架構（Controller / Service / DAO）進行模組化開發，搭配全域例外處理（@RestControllerAdvice）統一 API 回應格式，確保前端不會收到非預期的錯誤結構。透過自訂 LoggingFilter 整合 MDC Correlation ID，每筆請求自動注入唯一追蹤 ID，方便跨層日誌串聯排查。

### 9.事務效能：
 運用 @Transactional 確保跨 DAO 多步驟操作的原子性；以 Redis 分散式快取搭配 @Cacheable / @CacheEvict 對高頻查詢建立快取策略，第二次起相同請求不觸發 SQL 查詢，寫入時自動清除對應快取確保資料一致性。
 
 ### 10.安全設計： 
 以 BCrypt 加密儲存密碼，透過自訂 JWT Filter 實現無狀態身份驗證；登入成功後清除密碼欄位再回傳，防止敏感資料外洩。配置 Spring Security CORS 白名單，限定僅允許 GitHub Pages 前端跨域存取。


---

## 📡 API 列表

### 問卷管理（需 JWT）
| Method | Endpoint | 說明 |
|--------|----------|------|
| `GET` | `/quiz/getAll` | 取得所有問卷列表 |
| `GET` | `/quiz/get?quizId={id}` | 取得單筆問卷（含題目） |
| `POST` | `/quiz/create` | 建立新問卷 |
| `POST` | `/quiz/update` | 更新問卷（智慧差異更新） |
| `POST` | `/quiz/delete` | 批次刪除問卷 |

### 作答記錄
| Method | Endpoint | 說明 |
|--------|----------|------|
| `POST` | `/quiz/fillin` | 提交問卷作答 |
| `GET` | `/quiz/feedback?quizId={id}` | 取得所有作答統計 |

### 使用者系統
| Method | Endpoint | 說明 |
|--------|----------|------|
| `POST` | `/user/register` | 會員註冊（BCrypt 加密） |
| `POST` | `/user/login` | 登入（回傳 JWT Token） |
| `POST` | `/user/update` | 更新會員資料 |

---

## 🗄️ 資料庫設計

| 資料表 | 說明 | 主鍵 |
|--------|------|------|
| `quiz` | 問卷（標題、起訖日、發布狀態） | `id` AUTO_INCREMENT |
| `question` | 問題（題型、選項、必填） | `(quiz_id, question_id)` 複合 |
| `user` | 使用者（Email、加密密碼） | `email` |
| `fillin` | 作答記錄 | `(quiz_id, question_id, user_email)` 複合 |

---

## 🚀 本地啟動

### 環境需求
- Docker Desktop
- JDK 17（本地模式需要）

### 步驟

**1. 建立 `.env` 檔案**（和 `docker-compose.yml` 同層）
```
DB_NAME=quizdb
DB_PASSWORD=your_password
```

**2. 啟動所有容器**
```bash
docker compose up -d
```

**3. 確認啟動成功**
```bash
docker compose logs -f spring-app
# 看到 "Started Quiz1141121Application" 即成功
```

**4. 開啟 Swagger UI**
```
http://localhost:8080/swagger-ui.html
```

---

## 🔐 HTTPS Tunnel（ngrok + systemd）

**問題背景：** 前端部署於 GitHub Pages（`https://`），若後端只有裸 HTTP，瀏覽器會因混合內容（Mixed Content）限制而封鎖所有 API 請求。

**解法：** 在 EC2 上以 ngrok 建立永久 HTTPS Tunnel，並設定 `systemd` 服務讓 ngrok 開機自動啟動，確保後端始終可透過 HTTPS 存取：

```ini
# /etc/systemd/system/ngrok.service
[Unit]
Description=ngrok HTTPS tunnel
After=network.target

[Service]
ExecStart=/usr/bin/cloudflared tunnel --url http://localhost:8080
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl enable ngrok   # 設定開機自動啟動
sudo systemctl start ngrok    # 立即啟動
```

前端的 API Base URL 指向 ngrok 提供的 HTTPS 域名，即可解決 GitHub Pages 的混合內容限制。

---

## 👨‍💻 作者

**BeeSuperman**
- GitHub：[@BeeSuperman](https://github.com/BeeSuperman)
- 前端 Repo：[dynamic-questionnaire-frontend](https://github.com/BeeSuperman/dynamic-questionnaire-frontend)

---

<p align="center">Spring Boot 4 · Java 17 · Angular 19 · MySQL · Redis · Docker · AWS EC2 · GitHub Actions</p>
