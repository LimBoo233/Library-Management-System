// 文件: com/ILoveU/controller/AuthServlet.java
package com.ILoveU.servlet;

import com.ILoveU.dto.ApiErrorResponse;
import com.ILoveU.dto.UserDTO;
import com.ILoveU.exception.AuthenticationException;
import com.ILoveU.exception.DuplicateResourceException;
import com.ILoveU.exception.OperationFailedException;
import com.ILoveU.exception.ValidationException;
import com.ILoveU.service.Impl.UserServiceImpl;
import com.ILoveU.service.UserService;

import com.ILoveU.util.ServletUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;


import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;

import java.util.List;

// 仅用于logout的简单成功响应
import java.util.Map;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet("/api/auth/*")
public class AuthServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(AuthServlet.class);

    private UserService userService;

    @Override
    public void init() throws ServletException {
        super.init();
        this.userService = new UserServiceImpl(); // 实例化UserService
        logger.info("AuthServlet initialized."); // 使用SLF4J记录日志
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String pathInfo = request.getPathInfo();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        UserDTO successUserDTO = null; // 用于存储注册或登录成功的UserDTO
        Object successResponseObject = null; // 用于最终序列化为JSON的成功响应对象

        try {
            JsonObject jsonRequest = ServletUtil.getJsonFromRequestBody(request);

            String account = null;
            String password = null;
            String name = null; // 仅注册时需要

            if (jsonRequest != null) {
                // 安全地获取JSON属性
                if (jsonRequest.has("account") && !jsonRequest.get("account").isJsonNull()) {
                    account = jsonRequest.get("account").getAsString();
                }
                if (jsonRequest.has("password") && !jsonRequest.get("password").isJsonNull()) {
                    password = jsonRequest.get("password").getAsString();
                }
            }

            if ("/register".equals(pathInfo)) {
                if (jsonRequest != null && jsonRequest.has("username") && !jsonRequest.get("username").isJsonNull()) {
                    // API规范中注册请求体是 "username", "account", "password"
                    // 假设 "username" 对应我们User实体的 "name"
                    name = jsonRequest.get("username").getAsString(); // 根据API规范调整字段名
                } else if (jsonRequest != null && jsonRequest.has("name") && !jsonRequest.get("name").isJsonNull()) {
                    // 兼容旧的 "name" 字段，如果前端还未更新
                    name = jsonRequest.get("name").getAsString();
                }


                logger.info("Handling /register request for account: {}", account);
                successUserDTO = userService.registerUser(account, password, name);
                response.setStatus(HttpServletResponse.SC_CREATED); // 201 Created
                successResponseObject = successUserDTO; // 直接返回UserDTO，符合API规范

            } else if ("/login".equals(pathInfo)) {
                logger.info("Handling /login request for account: {}", account);
                successUserDTO = userService.loginUser(account, password);

                // 登录成功，创建Session
                HttpSession session = request.getSession(true);
                session.setAttribute("loggedInUser", successUserDTO); // 将UserDTO存入Session
                logger.info("Session created/updated for user: {}, Session ID: {}", successUserDTO.getAccount(), session.getId());

                response.setStatus(HttpServletResponse.SC_OK); // 200 OK

                // 根据API规范，登录成功返回 {"user": UserDTO} (因为我们不用JWT，所以token部分省略)
                Map<String, Object> tempMap = new HashMap<>();
                tempMap.put("user", successUserDTO);
                successResponseObject = tempMap;


            } else if ("/logout".equals(pathInfo)) {
                logger.info("Handling /logout request");
                HttpSession session = request.getSession(false);
                if (session != null) {
                    UserDTO loggedInUser = (UserDTO) session.getAttribute("loggedInUser");
                    String userAccountInSession = (loggedInUser != null) ? loggedInUser.getAccount() : "UnknownUser";
                    logger.info("Invalidating session for user: {}, Session ID: {}", userAccountInSession, session.getId());
                    session.invalidate();
                }
                response.setStatus(HttpServletResponse.SC_OK); // 200 OK

                // API规范未明确定义logout成功响应体，可以返回简单成功消息
                Map<String, Object> tempMap = new HashMap<>();
                tempMap.put("message", "用户已成功注销。");
                successResponseObject = tempMap;


            } else {
                logger.warn("AuthServlet: 未找到的请求路径: {}", pathInfo);
                sendErrorResponse(response, request, HttpServletResponse.SC_NOT_FOUND, "Not Found", "请求的认证接口未找到。");
                return; // 提前返回，不执行后续的成功响应发送
            }

            // 如果代码执行到这里，表示操作成功
            if (successResponseObject != null) {
                PrintWriter out = response.getWriter();
                out.print(ServletUtil.toJson(successResponseObject));
                out.flush();
            } else if (successUserDTO != null) {
                // 兜底，如果successResponseObject未被特定设置
                PrintWriter out = response.getWriter();
                out.print(ServletUtil.toJson(successUserDTO));
                out.flush();
            }


        } catch (ValidationException e) {
            logger.warn("Validation error for path {}: {}", request.getRequestURI(), e.getMessage());
            sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", e.getMessage(), e.getErrors());
        } catch (DuplicateResourceException e) {
            logger.warn("Duplicate resource error for path {}: {}", request.getRequestURI(), e.getMessage());
            // API规范中账号已存在是400 Bad Request
            sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", e.getMessage());
        } catch (AuthenticationException e) {
            logger.warn("Authentication failed for path {}: {}", request.getRequestURI(), e.getMessage());
            sendErrorResponse(response, request, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized", e.getMessage());
        } catch (OperationFailedException e) {
            logger.error("Operation failed for path {}: {}", request.getRequestURI(), e.getMessage(), e.getCause());
            sendErrorResponse(response, request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", e.getMessage());
        } catch (JsonSyntaxException e) {
            logger.error("JSON Syntax Error for path {}: {}", request.getRequestURI(), e.getMessage(), e);
            sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", "请求的JSON格式无效: " + e.getMessage());
        } catch (IOException e) { // 由ServletUtil.getJsonFromRequestBody抛出
            logger.error("IOException while reading request body for path {}: {}", request.getRequestURI(), e.getMessage(), e);
            sendErrorResponse(response, request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", "读取请求数据时发生错误。");
        } catch (Exception e) { // 捕获所有其他未预料的异常
            logger.error("Unexpected error for path {}: {}", request.getRequestURI(), e.getMessage(), e);
            sendErrorResponse(response, request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", "服务器发生意外错误，请稍后再试。");
        }
    }

    /**
     * 辅助方法，用于发送符合API规范的错误响应。
     */
    private void sendErrorResponse(HttpServletResponse response, HttpServletRequest request, int statusCode, String errorShortDescription, String message, List<ApiErrorResponse.FieldErrorDetail> fieldErrors) throws IOException {
        // 确保即使已经写入了部分响应（理论上不应该），也先重置
        ServletUtil.sendErrorResponse(response, request, statusCode, errorShortDescription, message, fieldErrors, logger);
    }

    // 重载一个不带字段错误详情的版本
    private void sendErrorResponse(HttpServletResponse response, HttpServletRequest request, int statusCode, String errorShortDescription, String message) throws IOException {
        ServletUtil.sendErrorResponse(response, request, statusCode, errorShortDescription, message, null);
    }


    @Override
    public void destroy() {
        super.destroy();
        logger.info("AuthServlet destroyed.");
    }
}
