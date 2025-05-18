package com.ILoveU.servlet;

import com.ILoveU.dto.LoanDTO;
import com.ILoveU.dto.PageDTO;

import com.ILoveU.exception.*;
import com.ILoveU.service.LoanService;
import com.ILoveU.service.Impl.LoanServiceImpl;
import com.ILoveU.util.ServletUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet("/api/loans/*")
public class LoanServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(LoanServlet.class);
    private LoanService loanService;

    @Override
    public void init() throws ServletException {
        super.init();
        this.loanService = new LoanServiceImpl();
        logger.info("LoanServlet initialized.");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String pathInfo = request.getPathInfo();

        try {
            JsonObject jsonRequest = ServletUtil.getJsonFromRequestBody(request);
            if (jsonRequest == null) {
                throw new ValidationException("请求体不能为空。");
            }

            if ("/checkout".equals(pathInfo)) {
                // POST /api/loans/checkout (借书)
                // API规范请求体: { "userId": 5, "bookId":124 }
                if (!jsonRequest.has("userId") || !jsonRequest.has("bookId")) {
                    throw new ValidationException("请求体必须包含userId和bookId。");
                }
                int userId = jsonRequest.get("userId").getAsInt();
                int bookId = jsonRequest.get("bookId").getAsInt();

                logger.info("Handling POST /api/loans/checkout - userId: {}, bookId: {}", userId, bookId);
                LoanDTO createdLoan = loanService.checkoutBook(userId, bookId);
                ServletUtil.sendSuccessResponse(response, HttpServletResponse.SC_CREATED, createdLoan);

            } else if ("/return".equals(pathInfo)) {
                // POST /api/loans/return (还书)
                // API规范请求体: { "loanId":501 }
                if (!jsonRequest.has("loanId")) {
                    throw new ValidationException("请求体必须包含loanId。");
                }
                int loanId = jsonRequest.get("loanId").getAsInt();

                logger.info("Handling POST /api/loans/return - loanId: {}", loanId);
                LoanDTO updatedLoan = loanService.returnBook(loanId);
                ServletUtil.sendSuccessResponse(response, HttpServletResponse.SC_OK, updatedLoan);

            } else {
                logger.warn("Invalid path for POST request: /api/loans{}", pathInfo);
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method Not Allowed", "此路径不支持POST请求。", logger);
            }
        } catch (ValidationException e) {
            logger.warn("Validation error in POST /api/loans: {}", e.getMessage());
            ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", e.getMessage(), e.getErrors(), logger);
        } catch (ResourceNotFoundException e) {
            logger.warn("Resource not found in POST /api/loans: {}", e.getMessage());
            ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_NOT_FOUND, "Not Found", e.getMessage(), logger);
        } catch (OperationForbiddenException e) {
            logger.warn("Operation forbidden in POST /api/loans: {}", e.getMessage());
            ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", e.getMessage(), logger); // 例如库存不足，API规范中可能是400
        } catch (OperationFailedException e) {
            logger.error("Operation failed in POST /api/loans: {}", e.getMessage(), e.getCause());
            ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", e.getMessage(), logger);
        } catch (JsonSyntaxException e) {
            logger.error("JSON Syntax Error in POST /api/loans: {}", e.getMessage(), e);
            ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", "请求的JSON格式无效: " + e.getMessage(), logger);
        } catch (IOException e) {
            logger.error("IOException in POST /api/loans: {}", e.getMessage(), e);
            ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", "读取请求数据时发生错误。", logger);
        } catch (Exception e) {
            logger.error("Unexpected error in POST /api/loans: {}", e.getMessage(), e);
            ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", "处理借阅请求时发生意外错误。", logger);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String pathInfo = request.getPathInfo();

        try {
            // 情况1: GET /api/loans (查询借阅记录, API规范可选功能, 支持?userId=5 或分页)
            if (pathInfo == null || pathInfo.equals("/")) {
                String userIdStr = request.getParameter("userId");
                String pageStr = request.getParameter("page");
                String pageSizeStr = request.getParameter("size");

                int page = (pageStr != null && !pageStr.isEmpty()) ? Integer.parseInt(pageStr) : 1;
                int pageSize = (pageSizeStr != null && !pageSizeStr.isEmpty()) ? Integer.parseInt(pageSizeStr) : 10;

                if (userIdStr != null && !userIdStr.isEmpty()) {
                    try {
                        int userId = Integer.parseInt(userIdStr);
                        logger.info("Handling GET /api/loans?userId={} - page: {}, pageSize: {}", userId, page, pageSize);
                        PageDTO<LoanDTO> pageResult = loanService.getLoansByUserId(userId, page, pageSize);
                        ServletUtil.sendSuccessResponse(response, HttpServletResponse.SC_OK, pageResult);
                    } catch (NumberFormatException e) {
                        logger.warn("无效的用户ID格式: {}", userIdStr, e);
                        ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", "用户ID格式无效。", logger);
                    }
                } else {
                    // 如果没有userId参数，根据API规范，这里可以不实现或返回错误，
                    logger.warn("GET /api/loans request without userId parameter.");
                    ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", "查询借阅记录需要提供userId参数。", logger);
                }
            // 情况2: GET /api/loans/{loanId} (获取指定借阅记录)
            } else if (pathInfo.matches("/\\d+")) { // 匹配 /数字
                String loanIdStr = pathInfo.substring(1);
                try {
                    int loanId = Integer.parseInt(loanIdStr);
                    logger.info("Handling GET /api/loans/{}", loanId);
                    LoanDTO loanDTO = loanService.getLoanById(loanId);
                    ServletUtil.sendSuccessResponse(response, HttpServletResponse.SC_OK, loanDTO);
                } catch (NumberFormatException e) {
                    logger.warn("无效的借阅记录ID格式: {}", loanIdStr, e);
                    ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", "借阅记录ID格式无效。", logger);
                }
            } else {
                logger.warn("Invalid path for GET request: /api/loans{}", pathInfo);
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_NOT_FOUND, "Not Found", "请求的借阅记录接口未找到。", logger);
            }
        } catch (ValidationException e) {
            logger.warn("Validation error in GET /api/loans: {}", e.getMessage());
            ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", e.getMessage(), e.getErrors(), logger);
        } catch (ResourceNotFoundException e) {
            logger.warn("Resource not found in GET /api/loans: {}", e.getMessage());
            ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_NOT_FOUND, "Not Found", e.getMessage(), logger);
        } catch (OperationFailedException e) {
            logger.error("Operation failed in GET /api/loans: {}", e.getMessage(), e.getCause());
            ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", e.getMessage(), logger);
        } catch (NumberFormatException e) { // 捕获分页参数转换的异常
            logger.warn("无效的分页参数格式: {}", e.getMessage());
            ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", "分页参数格式无效。", logger);
        } catch (Exception e) {
            logger.error("Unexpected error in GET /api/loans: {}", e.getMessage(), e);
            ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", "获取借阅信息时发生意外错误。", logger);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        logger.info("LoanServlet destroyed.");
    }
}
