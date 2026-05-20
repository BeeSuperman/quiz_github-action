package com.example.quiz_1141121.filter;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.quiz_1141121.util.JwtUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
// 讓 Spring 管理這個 Filter
public class JwtAuthFilter extends OncePerRequestFilter {
    // OncePerRequestFilter：保證每個 HTTP Request 只執行一次這個 Filter

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,   // 進來的 HTTP 請求
            HttpServletResponse response, // 要回傳的 HTTP 回應
            FilterChain filterChain       // 後續的 Filter 鏈
    ) throws ServletException, IOException {

        // 1. 從 Request Header 取出 Authorization 的值
        // 格式應該是："Bearer eyJhbGci..."
        String authHeader = request.getHeader("Authorization");

        // 2. 檢查 Header 是否存在且以 "Bearer " 開頭
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // 沒有 token 或格式不對 → 直接放行（讓 SecurityConfig 決定要不要擋）
            filterChain.doFilter(request, response);
            return;
        }

        // 3. 把 "Bearer " 前綴去掉，取出純粹的 token 字串
        // "Bearer eyJhbGci..." → "eyJhbGci..."
        String token = authHeader.substring(7);

        // 4. 驗證 token 是否有效
        if (!jwtUtil.isTokenValid(token)) {
            // token 無效（過期或被竄改）→ 直接放行，Security 會擋住
            filterChain.doFilter(request, response);
            return;
        }

        // 5. token 有效，從 token 取出 email
        String email = jwtUtil.extractEmail(token);

        // 6. 建立 Spring Security 的認證物件，告訴 Spring Security「這個 Request 已認證」
        // 參數：(principal身份, credentials憑證, authorities權限)
        // ⚠️ 學習用：這裡沒有設定 authorities（角色權限）
        // 公司實際做法：會從資料庫查出使用者角色（ROLE_USER, ROLE_ADMIN），放進 authorities
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(email, null, java.util.List.of());

        // 7. 把認證資訊存進 SecurityContext（Spring Security 的全域認證狀態）
        // 這樣後面的程式碼就知道「這個 Request 是誰發出的」
        SecurityContextHolder.getContext().setAuthentication(authToken);

        // 8. 放行，繼續往下一個 Filter 或 Controller 走
        filterChain.doFilter(request, response);
    }
}
