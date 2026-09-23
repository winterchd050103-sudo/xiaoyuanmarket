package com.xiaoyuan.market.common;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Security 过滤器链内直接写 JSON 响应
 */
public class JsonResponse {

    public static void write(HttpServletResponse response, Integer code, String message) throws IOException {
        response.setStatus(200);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"code\":" + code + ",\"message\":\""
                + (message == null ? "" : message.replace("\"", "'")) + "\",\"data\":null}");
    }
}
