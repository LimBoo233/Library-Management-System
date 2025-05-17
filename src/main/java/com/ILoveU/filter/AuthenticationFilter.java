package com.ILoveU.filter;

import com.ILoveU.dto.UserDTO;          // 引入UserDTO
import com.ILoveU.util.ServletUtil;     // 引入ServletUtil

// 使用 jakarta.servlet.* 因为你用的是Tomcat 10
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.annotation.WebInitParam;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * AuthenticationFilter 负责对需要认证的API请求进行统一的会话检查。
 * 它会拦截指定的URL模式，如果用户未登录（即HttpSession中没有有效的用户信息），
 * 则返回HTTP 401 Unauthorized错误，阻止请求到达目标Servlet。
 * 公共路径（如登录、注册）会被排除在检查之外。
 */
@WebFilter(
        filterName = "AuthenticationFilter",
        urlPatterns = {"/api/*"}, // 拦截所有以 /api/ 开头的请求
        initParams = {
                // 定义不需要认证的公共路径（相对于应用上下文）
                // 注意：这里的路径是相对于应用上下文的，并且不包含通配符本身
                // 例如，如果AuthServlet映射到 /api/auth/*，那么登录和注册路径是 /api/auth/login 和 /api/auth/register
                @WebInitParam(name = "publicPaths", value = "/api/auth/login,/api/auth/register")
        }
)
public class AuthenticationFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);
    private Set<String> publicPathsSet;

    /**
     * Filter初始化方法。
     * 在Filter第一次被创建时调用，用于读取初始化参数。
     * @param filterConfig Filter的配置对象
     * @throws ServletException 如果初始化失败
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("AuthenticationFilter initializing...");
        String publicPathsParam = filterConfig.getInitParameter("publicPaths");
        if (publicPathsParam != null && !publicPathsParam.isEmpty()) {
            publicPathsSet = new HashSet<>(Arrays.asList(publicPathsParam.split(",")));
            logger.info("Public paths configured: {}", publicPathsSet);
        } else {
            publicPathsSet = new HashSet<>();
            logger.info("No public paths configured for AuthenticationFilter.");
        }
        logger.info("AuthenticationFilter initialized successfully.");
    }

    /**
     * Filter的核心处理方法。
     * 每当一个匹配URL模式的请求到达时，此方法被调用。
     *
     * @param request  ServletRequest对象
     * @param response ServletResponse对象
     * @param chain    FilterChain对象，用于将请求传递给链中的下一个组件
     * @throws IOException 如果发生I/O错误
     * @throws ServletException 如果发生Servlet相关错误
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 获取请求的路径（不包含应用上下文路径）
        String path = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());
        // 有些容器的getRequestURI可能已经去掉了contextPath，需要根据实际情况调整
        // 或者更健壮的方式是直接使用 httpRequest.getServletPath() + (httpRequest.getPathInfo() == null ? "" : httpRequest.getPathInfo())
        // 但对于 /api/* 这样的模式，直接用 path 应该可以

        logger.debug("AuthenticationFilter processing request for path: {}", path);

        // 检查请求路径是否为公共路径
        if (isPublicPath(path)) {
            logger.debug("Path {} is public, allowing request to proceed.", path);
            chain.doFilter(request, response); // 公共路径，直接放行
            return;
        }

        // 对于非公共路径，检查Session
        HttpSession session = httpRequest.getSession(false); // false: 不自动创建新Session

        if (session == null || session.getAttribute("loggedInUser") == null) {
            // 用户未登录或Session无效/已过期
            logger.warn("Unauthorized access attempt to protected path: {}", path);
            // 设置响应类型和编码，因为ServletUtil.sendErrorResponse不再设置它们
            httpResponse.setContentType("application/json");
            httpResponse.setCharacterEncoding("UTF-8");
            ServletUtil.sendErrorResponse(httpResponse, httpRequest, HttpServletResponse.SC_UNAUTHORIZED,
                    "Unauthorized", "用户未登录或会话已过期，请先登录。", logger);
            return; // 阻止请求继续传递
        }

        // 用户已登录，允许请求继续传递给目标Servlet或下一个Filter
        UserDTO loggedInUser = (UserDTO) session.getAttribute("loggedInUser");
        logger.debug("User {} (ID: {}) is authorized for path: {}",
                loggedInUser.getAccount(), loggedInUser.getId(), path);
        chain.doFilter(request, response);
    }

    /**
     * 检查给定路径是否为配置的公共路径之一。
     * @param path 要检查的请求路径
     * @return 如果是公共路径则返回true，否则返回false
     */
    private boolean isPublicPath(String path) {
        if (publicPathsSet == null || publicPathsSet.isEmpty()) {
            return false;
        }
        // 精确匹配公共路径
        // 例如，如果 publicPathsSet 包含 "/api/auth/login"
        // 那么 path 必须完全等于 "/api/auth/login"
        // 如果你的公共路径也可能包含通配符或需要更复杂的匹配，这里的逻辑需要调整
        return publicPathsSet.contains(path);
    }

    /**
     * Filter销毁方法。
     * 在Filter实例被销毁前调用，用于释放资源。
     */
    @Override
    public void destroy() {
        logger.info("AuthenticationFilter destroyed.");
        // 清理资源 (如果需要)
    }
}
