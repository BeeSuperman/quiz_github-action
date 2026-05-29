package com.example.quiz_1141121.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class LoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);
    private static final String TRACE_ID = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 1. 產生 Correlation ID，注入 MDC
        //    取 UUID 前 8 碼：夠唯一又不會讓 log 太寬
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put(TRACE_ID, traceId);

        // 2. 記錄請求開始時間
        long startTime = System.currentTimeMillis();

        try {
            // 3. 放行請求，讓後續 Filter 和 Controller 繼續執行
            filterChain.doFilter(request, response);
        } finally {
            // 4. 請求完成後記錄 access log（不管成功或例外都執行）
            long duration = System.currentTimeMillis() - startTime;
            log.info("{} {} → {} ({}ms)",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    duration);

            // 5. 清除 MDC，防止 thread pool 複用時 traceId 殘留污染下一個請求
            MDC.clear();
        }
    }
}
