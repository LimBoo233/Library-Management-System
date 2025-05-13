// 建议放在项目的 com.example.util 包下
package com.ILoveU.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

// 根据你的Servlet容器版本选择正确的Servlet API包
// 如果是Tomcat 10+ (Jakarta EE 9+), 使用 jakarta.servlet
import jakarta.servlet.http.HttpServletRequest;

// 如果是Tomcat 9或更早版本 (Java EE 8), 使用 javax.servlet
// import javax.servlet.http.HttpServletRequest;

import java.io.BufferedReader;
import java.io.IOException;

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
}
