package com.ILoveU.servlet;

import com.ILoveU.service.UserService;
import com.ILoveU.service.Impl.UserServiceImpl;
import com.ILoveU.util.Log;
import com.ILoveU.util.ServletUtil;
import com.google.gson.Gson;
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
import java.util.HashMap;
import java.util.Map;

/**
 * AuthServlet负责处理用户认证相关的请求，包括：
 * - POST /api/auth/register : 用户注册
 * - POST /api/auth/login   : 用户登录
 * - POST /api/auth/logout  : 用户注销
 * 它使用HttpSession来管理用户会话。
 */
@WebServlet("/api/auth/*")
public class AuthServlet extends HttpServlet {

    private UserService userService;

    /**
     * Servlet初始化方法，在Servlet第一次被创建时调用。
     * 这里我们实例化UserService和Gson。
     *
     * @throws ServletException 如果初始化失败
     */
    @Override
    public void init() throws ServletException {
        super.init();
        this.userService = new UserServiceImpl();
        Log.Instance().info("AuthServlet initialized."); // 打印日志，确认初始化
    }

    /**
     * 处理POST请求。根据URL路径的不同部分（如 /register, /login, /logout）执行相应的操作。
     *
     * @param request  HttpServletRequest对象，包含客户端请求信息
     * @param response HttpServletResponse对象，用于向客户端发送响应
     * @throws ServletException 如果发生Servlet相关错误
     * @throws IOException      如果发生输入输出错误
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 获取URL中 /api/auth/* 后面的部分，例如 "/register"
        String pathInfo = request.getPathInfo();

        // 设置响应内容类型为JSON，编码为UTF-8
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // 获取用于向客户端发送响应的PrintWriter
        PrintWriter out = response.getWriter();

        // 用于存储UserService返回的结果
        Map<String, Object> serviceResult = null;
        // 标记登录是否成功，以便后续创建Session
        boolean loginSuccess = false;
        // 登录成功后要存入Session的用户信息
        Map<String, Object> userToStoreInSession = null;

        try {
            JsonObject requestBody = ServletUtil.getJsonFromRequestBody(request);

            String account = null, password = null, name = null;

            if (requestBody != null) {
                account = requestBody.get("account").getAsString();
                password = requestBody.get("password").getAsString();
            }

            // 根据pathInfo判断执行哪个操作
            if ("/register".equals(pathInfo)) {
                if (requestBody != null) {
                    name = requestBody.has("name") ? requestBody.get("name").getAsString() : null;
                }
                Log.Instance().info("Handling /register request for account: " + account);
                serviceResult = userService.registerUser(account, password, name);
            } else if ("/login".equals(pathInfo)) {
                serviceResult = userService.loginUser(account, password);
                // 检查登录是否成功，并获取用户信息以便存入Session
                if ((Boolean) serviceResult.get("success")) {
                    loginSuccess = true;
                    userToStoreInSession = (Map<String, Object>) serviceResult.get("user");
                }
            } else if ("/logout".equals(pathInfo)) {
                // 获取现有Session，不创建新的
                HttpSession session = request.getSession(false);

                if (session != null) {
                    String userAccountInSession = "UnknownUser";
                    if (session.getAttribute("loggedInUser") != null) {
                        Map<String, Object> loggedInUser = (Map<String, Object>) session.getAttribute("loggedInUser");
                        userAccountInSession = (String) loggedInUser.get("account");
                    }
                    Log.Instance().info("Invalidating session for user: " + userAccountInSession + ", Session ID: " + session.getId());
                    // 使Session失效
                    session.invalidate();
                }

                serviceResult = new HashMap<>();
                serviceResult.put("success", true);
                serviceResult.put("message", "用户已成功注销。");
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                Log.Instance().severe("AuthServlet: 意外请求路径\n" + pathInfo);
            }
        } catch (JsonSyntaxException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            Log.Instance().severe("AuthServlet: JSON解析错误\n" + e.getMessage());
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            Log.Instance().severe("AuthServlet: 获取请求体失败\n" + e.getMessage());
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            Log.Instance().severe("AuthServlet: 发生意外错误\n" + e.getMessage());
        }

        // 如果登录成功，创建或获取HttpSession并存储用户信息
        if (loginSuccess && userToStoreInSession != null) {
            // true表示如果session不存在则创建
            HttpSession session = request.getSession(true);
            // 将用户信息Map存入session
            session.setAttribute("loggedInUser", userToStoreInSession);
            Log.Instance().info("Session created/updated for user: " + userToStoreInSession.get("account") + ", Session ID: " + session.getId()); // 日志
        }


        if (serviceResult == null) {
            serviceResult = new HashMap<>();
            serviceResult.put("success", false);
            serviceResult.put("message", "无效的请求路径或发生错误");
        }

        // 将Service层返回的Map转换为JSON字符串并发送给客户端
        out.print(ServletUtil.toJson(serviceResult));

        // 确保所有缓冲的输出都被发送
        out.flush();
    }



    @Override
    public void destroy() {
        super.destroy();
        Log.Instance().info("AuthServlet destroyed.");
    }

}
