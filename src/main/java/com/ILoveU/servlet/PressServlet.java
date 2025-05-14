package com.ILoveU.servlet;

import com.ILoveU.dto.ApiErrorResponse;
import com.ILoveU.dto.PageDTO;
import com.ILoveU.dto.PressDTO;
import com.ILoveU.exception.*;
import com.ILoveU.service.Impl.PressServiceImpl;
import com.ILoveU.service.PressService;
import com.ILoveU.util.ServletUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet("/api/presses/*")
public class PressServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(PressServlet.class);
    private PressService pressService;

    @Override
    public void init() throws ServletException {
        super.init();
        pressService = new PressServiceImpl();
        logger.info("PressServlet initialized.");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String pathInfo = request.getPathInfo();

        try {
            // 情况1: GET /api/presses (获取出版社列表，支持分页)
            if (pathInfo == null || pathInfo.equals("/")) {
                String pageStr = request.getParameter("page");
                String pageSizeStr = request.getParameter("pageSize");

                // API规范中分页参数默认值: page=1, size=10
                int page = (pageStr != null && !pageStr.isEmpty()) ? Integer.parseInt(pageStr) : 1;
                int pageSize = (pageSizeStr != null && !pageSizeStr.isEmpty()) ? Integer.parseInt(pageSizeStr) : 10;

                logger.info("Handling GET /api/presses - page: {}, pageSize: {}", page, pageSize);
                PageDTO<PressDTO> pageResult = pressService.getPressesWithPagination(page, pageSize);
                sendSuccessResponse(response, HttpServletResponse.SC_OK, pageResult);

                // 情况2: GET /api/presses/{pressId} (获取指定出版社)
            } else {
                String pressIdStr = pathInfo.substring(1); // 移除开头的 '/'
                try {
                    int pressId = Integer.parseInt(pressIdStr);
                    logger.info("Handling GET /api/presses/{}", pressId);
                    PressDTO pressDTO = pressService.getPressById(pressId);
                    sendSuccessResponse(response, HttpServletResponse.SC_OK, pressDTO);
                } catch (NumberFormatException e) {
                    logger.warn("无效的出版社ID格式: {}", pressIdStr, e);
                    sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", "出版社ID格式无效。");
                }
            }
        } catch (ValidationException e) {
            logger.warn("Validation error in GET /api/presses: {}", e.getMessage());
            sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", e.getMessage(), e.getErrors());
        } catch (ResourceNotFoundException e) {
            logger.warn("Resource not found in GET /api/presses: {}", e.getMessage());
            sendErrorResponse(response, request, HttpServletResponse.SC_NOT_FOUND, "Not Found", e.getMessage());
        } catch (OperationFailedException e) {
            logger.error("Operation failed in GET /api/presses: {}", e.getMessage(), e.getCause());
            sendErrorResponse(response, request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", e.getMessage());
        } catch (NumberFormatException e) { // 捕获分页参数转换的异常
            logger.warn("无效的分页参数格式: {}", e.getMessage());
            sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", "分页参数格式无效。");
        } catch (Exception e) {
            logger.error("Unexpected error in GET /api/presses: {}", e.getMessage(), e);
            sendErrorResponse(response, request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", "获取出版社信息时发生意外错误。");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String pathInfo = request.getPathInfo();

        // POST /api/presses (创建新出版社)
        if (pathInfo == null || pathInfo.equals("/")) {
            try {
                JsonObject jsonRequest = ServletUtil.getJsonFromRequestBody(request);
                if (jsonRequest == null) {
                    throw new ValidationException("请求体不能为空。");
                }
                // API规范规定，创建出版社时，请求体只需要name
                // 但PressDTO包含id和name，所以此处创建一个只包含name的DTO或直接传递name可能更合适
                // 假设PressDTO可以用于创建，Service层会处理ID为null的情况
                String name = jsonRequest.has("name") ? jsonRequest.get("name").getAsString() : null;

                PressDTO pressToCreate = new PressDTO(0, name); // ID设为0或null，由Service处理

                logger.info("Handling POST /api/presses with name: {}", name);
                PressDTO createdPress = pressService.createNewPress(pressToCreate);
                sendSuccessResponse(response, HttpServletResponse.SC_CREATED, createdPress);

            } catch (ValidationException e) {
                logger.warn("Validation error in POST /api/presses: {}", e.getMessage());
                sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", e.getMessage(), e.getErrors());
            } catch (DuplicateResourceException e) {
                logger.warn("Duplicate resource error in POST /api/presses: {}", e.getMessage());
                sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", e.getMessage()); // API规范中重复是400
            } catch (OperationFailedException e) {
                logger.error("Operation failed in POST /api/presses: {}", e.getMessage(), e.getCause());
                sendErrorResponse(response, request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", e.getMessage());
            } catch (JsonSyntaxException e) {
                logger.error("JSON Syntax Error in POST /api/presses: {}", e.getMessage(), e);
                sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", "请求的JSON格式无效: " + e.getMessage());
            } catch (IOException e) {
                logger.error("IOException in POST /api/presses: {}", e.getMessage(), e);
                sendErrorResponse(response, request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", "读取请求数据时发生错误。");
            } catch (Exception e) {
                logger.error("Unexpected error in POST /api/presses: {}", e.getMessage(), e);
                sendErrorResponse(response, request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", "创建出版社时发生意外错误。");
            }
        } else {
            logger.warn("Invalid path for POST request: /api/presses{}", pathInfo);
            sendErrorResponse(response, request, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method Not Allowed", "此路径不支持POST请求。");
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String pathInfo = request.getPathInfo();

        // PUT /api/presses/{pressId} (更新出版社)
        if (pathInfo != null && pathInfo.matches("/\\d+")) { // 匹配 /数字
            String pressIdStr = pathInfo.substring(1);
            try {
                int pressId = Integer.parseInt(pressIdStr);
                JsonObject jsonRequest = ServletUtil.getJsonFromRequestBody(request);
                if (jsonRequest == null) {
                    throw new ValidationException("请求体不能为空。");
                }
                String name = jsonRequest.has("name") ? jsonRequest.get("name").getAsString() : null;
                PressDTO pressToUpdate = new PressDTO(pressId, name); // DTO的ID可以忽略或用于验证

                logger.info("Handling PUT /api/presses/{} with name: {}", pressId, name);
                PressDTO updatedPress = pressService.updateExistingPress(pressId, pressToUpdate);
                sendSuccessResponse(response, HttpServletResponse.SC_OK, updatedPress);

            } catch (NumberFormatException e) {
                logger.warn("无效的出版社ID格式: {}", pressIdStr, e);
                sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", "出版社ID格式无效。");
            } catch (ValidationException e) {
                logger.warn("Validation error in PUT /api/presses: {}", e.getMessage());
                sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", e.getMessage(), e.getErrors());
            } catch (ResourceNotFoundException e) {
                logger.warn("Resource not found in PUT /api/presses: {}", e.getMessage());
                sendErrorResponse(response, request, HttpServletResponse.SC_NOT_FOUND, "Not Found", e.getMessage());
            } catch (DuplicateResourceException e) {
                logger.warn("Duplicate resource error in PUT /api/presses: {}", e.getMessage());
                sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", e.getMessage());
            } catch (OperationFailedException e) {
                logger.error("Operation failed in PUT /api/presses: {}", e.getMessage(), e.getCause());
                sendErrorResponse(response, request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", e.getMessage());
            } catch (JsonSyntaxException e) {
                logger.error("JSON Syntax Error in PUT /api/presses: {}", e.getMessage(), e);
                sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", "请求的JSON格式无效: " + e.getMessage());
            } catch (IOException e) {
                logger.error("IOException in PUT /api/presses: {}", e.getMessage(), e);
                sendErrorResponse(response, request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", "读取请求数据时发生错误。");
            } catch (Exception e) {
                logger.error("Unexpected error in PUT /api/presses: {}", e.getMessage(), e);
                sendErrorResponse(response, request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", "更新出版社时发生意外错误。");
            }
        } else {
            logger.warn("Invalid path for PUT request: /api/presses{}", pathInfo);
            sendErrorResponse(response, request, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method Not Allowed", "PUT请求需要指定出版社ID。");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json"); // 虽然204不返回内容，但设置一下无妨
        response.setCharacterEncoding("UTF-8");
        String pathInfo = request.getPathInfo();

        // DELETE /api/presses/{pressId} (删除出版社)
        if (pathInfo != null && pathInfo.matches("/\\d+")) { // 匹配 /数字
            String pressIdStr = pathInfo.substring(1);
            try {
                int pressId = Integer.parseInt(pressIdStr);
                logger.info("Handling DELETE /api/presses/{}", pressId);
                pressService.deletePressById(pressId);
                response.setStatus(HttpServletResponse.SC_NO_CONTENT); // 204 No Content for successful deletion

            } catch (NumberFormatException e) {
                logger.warn("无效的出版社ID格式: {}", pressIdStr, e);
                sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", "出版社ID格式无效。");
            } catch (ResourceNotFoundException e) {
                logger.warn("Resource not found in DELETE /api/presses: {}", e.getMessage());
                sendErrorResponse(response, request, HttpServletResponse.SC_NOT_FOUND, "Not Found", e.getMessage());
            } catch (OperationForbiddenException e) {
                logger.warn("Operation forbidden in DELETE /api/presses: {}", e.getMessage());
                // 根据API规范，如果删除因业务规则（如有关联书籍）被禁止，可以返回400或409 Conflict
                sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", e.getMessage());
            } catch (OperationFailedException e) {
                logger.error("Operation failed in DELETE /api/presses: {}", e.getMessage(), e.getCause());
                sendErrorResponse(response, request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", e.getMessage());
            } catch (Exception e) {
                logger.error("Unexpected error in DELETE /api/presses: {}", e.getMessage(), e);
                sendErrorResponse(response, request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", "删除出版社时发生意外错误。");
            }
        } else {
            logger.warn("Invalid path for DELETE request: /api/presses{}", pathInfo);
            sendErrorResponse(response, request, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method Not Allowed", "DELETE请求需要指定出版社ID。");
        }
    }

    /**
     * 辅助方法，用于发送成功的JSON响应。
     */
    private void sendSuccessResponse(HttpServletResponse response, int statusCode, Object data) throws IOException {
        response.setStatus(statusCode);
        if (data != null) { // 对于204 No Content，data可能是null
            PrintWriter out = response.getWriter();
            out.print(ServletUtil.toJson(data));
            out.flush();
        }
    }


    private void sendErrorResponse(HttpServletResponse response, HttpServletRequest request, int statusCode, String errorShortDescription, String message, List<ApiErrorResponse.FieldErrorDetail> fieldErrors) throws IOException {
        ServletUtil.sendErrorResponse(response, request, statusCode, errorShortDescription, message, fieldErrors, logger);
    }

    private void sendErrorResponse(HttpServletResponse response, HttpServletRequest request, int statusCode, String errorShortDescription, String message) throws IOException {
        sendErrorResponse(response, request, statusCode, errorShortDescription, message, null);
    }

    @Override
    public void destroy() {
        super.destroy();

    }

}
