package com.example.quiz_1141121.util;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
// 讓 Spring 管理這個類，可以用 @Autowired 注入
public class JwtUtil {

    // 簽名用的密鑰，必須夠長（至少 32 字元）
    // ⚠️ 學習用：密鑰直接寫在程式碼裡
    // 公司實際做法：從環境變數或 AWS Secrets Manager 取得，不寫在程式碼裡
    private static final String SECRET = "quiz-app-secret-key-must-be-long-enough-32chars";

    // Token 有效時間：24 小時（單位：毫秒）
    // 1000毫秒 × 60秒 × 60分 × 24小時 = 86400000 毫秒
    private static final long EXPIRATION_MS = 86400000L;

    // 把 SECRET 字串轉成 SecretKey 物件（jjwt 0.12.x 的用法）
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    /**
     * 產生 JWT Token
     * @param email 使用者的 email（放進 token 的 subject）
     * @return 產生的 token 字串，例如 "eyJhbGci..."
     */
    public String generateToken(String email) {
        return Jwts.builder()
                .subject(email)              // 把 email 存進 token（誰的 token）
                .issuedAt(new Date())         // 發行時間（現在）
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS)) // 過期時間
                .signWith(getSigningKey())    // 用密鑰簽名，防止偽造
                .compact();                   // 產生最終的 token 字串
    }

    /**
     * 從 Token 取出 email
     * @param token JWT token 字串
     * @return email 字串
     */
    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
        // Claims 是 token 的 payload 部分
        // getSubject() 取出我們存進去的 email
    }

    /**
     * 驗證 Token 是否有效（沒過期、簽名正確）
     * @param token JWT token 字串
     * @return true = 有效，false = 無效或過期
     */
    public boolean isTokenValid(String token) {
        try {
            extractClaims(token); // 如果 token 無效或過期，這行會丟出例外
            return true;
        } catch (Exception e) {
            return false; // 任何例外都代表 token 無效
        }
    }

    // 解析 token，取得 Claims（payload 的所有內容）
    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey()) // 用相同的密鑰驗證簽名
                .build()
                .parseSignedClaims(token)    // 解析 token
                .getPayload();               // 取得 payload（Claims）
    }
}
