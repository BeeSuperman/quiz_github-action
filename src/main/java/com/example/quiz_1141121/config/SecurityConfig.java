package com.example.quiz_1141121.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.example.quiz_1141121.filter.JwtAuthFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
// 告訴 Spring：這個類有 @Bean 方法，要把回傳值放進 Spring 容器
@EnableWebSecurity
// 啟用 Spring Security 的網頁安全功能
public class SecurityConfig {

    @Autowired
    private JwtAuthFilter jwtAuthFilter;
    // 注入我們自己寫的 JWT Filter

    @Bean
    // 這個方法的回傳值會被 Spring 管理
    // SecurityFilterChain：定義整個安全規則的核心物件
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 1. 設定 CORS：允許指定來源的跨域請求，必須在 Security filter 層處理，@CrossOrigin 來不及
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // 2. 關閉 CSRF 保護
            // CSRF（跨站請求偽造）是給「有 Session 的瀏覽器應用」用的防護
            // 我們用 JWT（無狀態），不需要 CSRF 保護
            .csrf(csrf -> csrf.disable())

            // 2. 設定 Session 管理策略
            // STATELESS：不建立 Session，每個 Request 都靠 JWT 自己驗證
            // ⚠️ 學習用：這是 JWT 的標準設定
            // 公司實際做法也一樣，JWT 就是為了無狀態而生的
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 3. 設定哪些路徑需要驗證，哪些不需要
            .authorizeHttpRequests(auth -> auth
                // 以下路徑「不需要」登入就能訪問
                .requestMatchers(
                    "/user/login",           // 登入
                    "/user/register",        // 註冊
                    "/v3/api-docs/**",       // Swagger API 文件
                    "/swagger-ui/**",        // Swagger UI
                    "/swagger-ui.html"       // Swagger UI 首頁
                ).permitAll()               // 全部放行

                // 其他所有路徑都需要登入（帶有效 JWT token）
                .anyRequest().authenticated()
            )

            // 4. 設定例外處理：沒有 token 時回傳 401（而非 Spring 預設的 403）
            // AuthenticationEntryPoint：當「未認證」時觸發（沒帶 token 或 token 無效）
            // ⚠️ 學習用：這裡直接寫 lambda，公司實際做法會獨立成一個 EntryPoint 類別
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) ->
                    response.sendError(401, "Unauthorized: Please provide a valid JWT token")
                )
            )

            // 5. 把我們的 JwtAuthFilter 加進 Filter 鏈
            // 放在 UsernamePasswordAuthenticationFilter 之前
            // 這樣每個 Request 進來，會先經過 JwtAuthFilter 驗證 token
            // 再決定要不要放行到 Controller
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(
            "http://localhost:4200",
            "https://beesuperman.github.io"
        ));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
