// 建议放在项目的 com.example.util 包下
package com.ILoveU.util;

import com.ILoveU.dto.ApiErrorResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

// 根据你的Servlet容器版本选择正确的Servlet API包
// 如果是Tomcat 10+ (Jakarta EE 9+), 使用 jakarta.servlet
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;

// 如果是Tomcat 9或更早版本 (Java EE 8), 使用 javax.servlet
// import javax.servlet.http.HttpServletRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class ServletUtil {

    private static final Gson gson = new Gson(); // Gson实例，可以在工具类中共享

    /**
     * 从HttpServletRequest中读取请求体，并将其解析为JsonObject。
     *
     * @param request HttpServletRequest 对象
     * @return 解析后的 JsonObject，如果请求体为空或解析失败则返回null或抛出异常（当前实现为返回null）
     * @throws IOException 如果读取请求体时发生I/O错误
     * @throws JsonSyntaxException 如果请求体不是有效的JSON格式
     */
    public static JsonObject getJsonFromRequestBody(HttpServletRequest request) throws IOException, JsonSyntaxException {
        StringBuilder sb = new StringBuilder();

        // 确保reader被关闭，即使发生异常
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        String requestBody = sb.toString();

        if (requestBody.trim().isEmpty()) {
            // 或者根据需要返回一个空的JsonObject: new JsonObject()
            return null;
        }

        return gson.fromJson(requestBody, JsonObject.class);
    }

    /**
     * 将Java对象转换为JSON字符串。
     * @param object 要转换的对象
     * @return JSON字符串
     */
    public static String toJson(Object object) {
        return gson.toJson(object);
    }

    /**
     * 发送符合API规范的错误响应。
     *
     * @param response HttpServletResponse 对象
     * @param request HttpServletRequest 对象，用于获取请求路径
     * @param statusCode HTTP状态码
     * @param errorShortDescription 状态码的简短描述 (例如 "Not Found")
     * @param message 具体的错误信息
     * @param fieldErrors 可选的字段级别错误列表
     * @param logger 用于记录错误的Logger实例 (通常是调用方Servlet的logger)
     * @throws IOException 如果写入响应时发生I/O错误
     */
    public static void sendErrorResponse(HttpServletResponse response, HttpServletRequest request, int statusCode, String errorShortDescription, String message, List<ApiErrorResponse.FieldErrorDetail> fieldErrors, Logger logger) throws IOException {
        if (response.isCommitted()) {
            logger.error("Response already committed. Cannot send error response for status {} and message: {}", statusCode, message);
            return;
        }
        response.setStatus(statusCode);

        ApiErrorResponse errorResponsePojo = new ApiErrorResponse(statusCode, errorShortDescription, message, request.getRequestURI(), fieldErrors);

        PrintWriter out = response.getWriter();
        out.print(toJson(errorResponsePojo));
        out.flush();
    }

    /**
     * 发送符合API规范的错误响应 (不带字段级别错误列表的重载版本)。
     *
     * @param response HttpServletResponse 对象
     * @param request HttpServletRequest 对象
     * @param statusCode HTTP状态码
     * @param errorShortDescription 状态码的简短描述
     * @param message 具体的错误信息
     * @param logger 用于记录错误的Logger实例
     * @throws IOException 如果写入响应时发生I/O错误
     */
    public static void sendErrorResponse(HttpServletResponse response, HttpServletRequest request, int statusCode, String errorShortDescription, String message, Logger logger) throws IOException {
        ServletUtil.sendErrorResponse(response, request, statusCode, errorShortDescription, message, null, logger);
    }

    /**
     * 发送成功的JSON响应。
     *
     * @param response HttpServletResponse 对象
     * @param statusCode HTTP状态码 (例如 200 OK, 201 Created)
     * @param data 要序列化为JSON并发送的数据对象。如果为null且状态码不是204, 则发送空JSON对象。
     * 对于204 No Content, data应为null，且不发送响应体。
     * @throws IOException 如果写入响应时发生I/O错误
     */
    public static void sendSuccessResponse(HttpServletResponse response, int statusCode, Object data) throws IOException {
        // ContentType 和 CharacterEncoding 应该在调用此方法前，在Servlet中统一设置
        // 例如在Servlet的doXxx方法开头:
        // response.setContentType("application/json");
        // response.setCharacterEncoding("UTF-8");
        response.setStatus(statusCode);

        // 对于 204 No Content，不应有响应体
        if (statusCode == HttpServletResponse.SC_NO_CONTENT) {
            return; // 直接返回，不写入任何内容
        }

        PrintWriter out = response.getWriter();
        if (data != null) {
            out.print(toJson(data));
        } else {
            // 如果data为null但不是204，可以根据API规范决定是发送空JSON对象{}还是空字符串
            // 通常发送空JSON对象更符合JSON API的习惯
            out.print("{}");
        }
        out.flush();
    }
}
