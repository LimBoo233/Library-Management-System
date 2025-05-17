package com.ILoveU.servlet;

import com.ILoveU.dto.ApiErrorResponse;
import com.ILoveU.dto.PageDTO;
import com.ILoveU.dto.TagDTO;

import com.ILoveU.exception.*;
import com.ILoveU.dao.impl.BookDAOImpl;
import com.ILoveU.dao.impl.TagDAOImpl;
import com.ILoveU.service.TagService;
import com.ILoveU.service.Impl.TagServiceImpl;
import com.ILoveU.util.ServletUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet("/api/tags/*")
public class TagServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(TagServlet.class);
    private TagService tagService;

    @Override
    public void init() throws ServletException {
        super.init();
        this.tagService = new TagServiceImpl();
        logger.info("TagServlet initialized.");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String pathInfo = request.getPathInfo();

        try {
            // 情况1: GET /api/tags (获取标签列表，支持分页)
            // API规范中未明确Tag列表是否分页，但通常列表接口会支持分页
            if (pathInfo == null || pathInfo.equals("/")) {
                String pageStr = request.getParameter("page");
                String pageSizeStr = request.getParameter("size");

                int page = (pageStr != null && !pageStr.isEmpty()) ? Integer.parseInt(pageStr) : 1;
                int pageSize = (pageSizeStr != null && !pageSizeStr.isEmpty()) ? Integer.parseInt(pageSizeStr) : 10; // 默认每页10条

                logger.info("Handling GET /api/tags - page: {}, pageSize: {}", page, pageSize);
                PageDTO<TagDTO> pageResult = tagService.getTags(page, pageSize);
                ServletUtil.sendSuccessResponse(response, HttpServletResponse.SC_OK, pageResult);

            // 情况2: GET /api/tags/{tagId} (获取指定标签)
            } else {
                String tagIdStr = pathInfo.substring(1); // 移除开头的 '/'
                try {
                    int tagId = Integer.parseInt(tagIdStr);
                    logger.info("Handling GET /api/tags/{}", tagId);
                    TagDTO tagDTO = tagService.getTagById(tagId);
                    ServletUtil.sendSuccessResponse(response, HttpServletResponse.SC_OK, tagDTO);
                } catch (NumberFormatException e) {
                    logger.warn("无效的标签ID格式: {}", tagIdStr, e);
                    ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", "标签ID格式无效。", logger);
                }
            }
        } catch (ValidationException e) {
            logger.warn("Validation error in GET /api/tags: {}", e.getMessage());
            ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", e.getMessage(), e.getErrors(), logger);
        } catch (ResourceNotFoundException e) {
            logger.warn("Resource not found in GET /api/tags: {}", e.getMessage());
            ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_NOT_FOUND, "Not Found", e.getMessage(), logger);
        } catch (OperationFailedException e) {
            logger.error("Operation failed in GET /api/tags: {}", e.getMessage(), e.getCause());
            ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", e.getMessage(), logger);
        } catch (NumberFormatException e) { // 捕获分页参数转换的异常
            logger.warn("无效的分页参数格式: {}", e.getMessage());
            ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", "分页参数格式无效。", logger);
        } catch (Exception e) {
            logger.error("Unexpected error in GET /api/tags: {}", e.getMessage(), e);
            ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", "获取标签信息时发生意外错误。", logger);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String pathInfo = request.getPathInfo();

        // POST /api/tags (创建新标签)
        if (pathInfo == null || pathInfo.equals("/")) {
            try {
                JsonObject jsonRequest = ServletUtil.getJsonFromRequestBody(request);
                if (jsonRequest == null) {
                    throw new ValidationException("请求体不能为空。");
                }
                // API规范中Tag对象仅含id和name，创建时客户端应只提供name
                String name = jsonRequest.has("name") ? jsonRequest.get("name").getAsString() : null;

                TagDTO tagToCreate = new TagDTO(null, name); // ID为null，由服务器生成

                logger.info("Handling POST /api/tags with name: {}", name);
                TagDTO createdTag = tagService.createTag(tagToCreate);
                ServletUtil.sendSuccessResponse(response, HttpServletResponse.SC_CREATED, createdTag);

            } catch (ValidationException e) {
                logger.warn("Validation error in POST /api/tags: {}", e.getMessage());
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", e.getMessage(), e.getErrors(), logger);
            } catch (DuplicateResourceException e) {
                logger.warn("Duplicate resource error in POST /api/tags: {}", e.getMessage());
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", e.getMessage(), logger); // 标签名重复是400
            } catch (OperationFailedException e) {
                logger.error("Operation failed in POST /api/tags: {}", e.getMessage(), e.getCause());
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", e.getMessage(), logger);
            } catch (JsonSyntaxException e) {
                logger.error("JSON Syntax Error in POST /api/tags: {}", e.getMessage(), e);
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", "请求的JSON格式无效: " + e.getMessage(), logger);
            } catch (IOException e) {
                logger.error("IOException in POST /api/tags: {}", e.getMessage(), e);
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", "读取请求数据时发生错误。", logger);
            } catch (Exception e) {
                logger.error("Unexpected error in POST /api/tags: {}", e.getMessage(), e);
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", "创建标签时发生意外错误。", logger);
            }
        } else {
            logger.warn("Invalid path for POST request: /api/tags{}", pathInfo);
            ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method Not Allowed", "此路径不支持POST请求。", logger);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String pathInfo = request.getPathInfo();

        // PUT /api/tags/{tagId} (更新标签)
        if (pathInfo != null && pathInfo.matches("/\\d+")) { // 匹配 /数字
            String tagIdStr = pathInfo.substring(1);
            try {
                int tagId = Integer.parseInt(tagIdStr);
                JsonObject jsonRequest = ServletUtil.getJsonFromRequestBody(request);
                if (jsonRequest == null) {
                    throw new ValidationException("请求体不能为空。");
                }
                String name = jsonRequest.has("name") ? jsonRequest.get("name").getAsString() : null;
                TagDTO tagToUpdate = new TagDTO(tagId, name); // DTO的ID用于Service层校验，但主要以路径ID为准

                logger.info("Handling PUT /api/tags/{} with name: {}", tagId, name);
                TagDTO updatedTag = tagService.updateTag(tagId, tagToUpdate);
                ServletUtil.sendSuccessResponse(response, HttpServletResponse.SC_OK, updatedTag);

            } catch (NumberFormatException e) {
                logger.warn("无效的标签ID格式: {}", tagIdStr, e);
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", "标签ID格式无效。", logger);
            } catch (ValidationException e) {
                logger.warn("Validation error in PUT /api/tags: {}", e.getMessage());
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", e.getMessage(), e.getErrors(), logger);
            } catch (ResourceNotFoundException e) {
                logger.warn("Resource not found in PUT /api/tags: {}", e.getMessage());
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_NOT_FOUND, "Not Found", e.getMessage(), logger);
            } catch (DuplicateResourceException e) {
                logger.warn("Duplicate resource error in PUT /api/tags: {}", e.getMessage());
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", e.getMessage(), logger);
            } catch (OperationFailedException e) {
                logger.error("Operation failed in PUT /api/tags: {}", e.getMessage(), e.getCause());
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", e.getMessage(), logger);
            } catch (JsonSyntaxException e) {
                logger.error("JSON Syntax Error in PUT /api/tags: {}", e.getMessage(), e);
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", "请求的JSON格式无效: " + e.getMessage(), logger);
            } catch (IOException e) {
                logger.error("IOException in PUT /api/tags: {}", e.getMessage(), e);
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", "读取请求数据时发生错误。", logger);
            } catch (Exception e) {
                logger.error("Unexpected error in PUT /api/tags: {}", e.getMessage(), e);
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", "更新标签时发生意外错误。", logger);
            }
        } else {
            logger.warn("Invalid path for PUT request: /api/tags{}", pathInfo);
            ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method Not Allowed", "PUT请求需要指定标签ID。", logger);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setCharacterEncoding("UTF-8"); // 204通常不设置ContentType，但设置编码无妨

        String pathInfo = request.getPathInfo();

        // DELETE /api/tags/{tagId} (删除标签)
        if (pathInfo != null && pathInfo.matches("/\\d+")) { // 匹配 /数字
            String tagIdStr = pathInfo.substring(1);
            try {
                int tagId = Integer.parseInt(tagIdStr);
                logger.info("Handling DELETE /api/tags/{}", tagId);
                tagService.deleteTag(tagId);
                response.setStatus(HttpServletResponse.SC_NO_CONTENT); // 204 No Content for successful deletion

            } catch (NumberFormatException e) {
                logger.warn("无效的标签ID格式: {}", tagIdStr, e);
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", "标签ID格式无效。", logger);
            } catch (ResourceNotFoundException e) {
                logger.warn("Resource not found in DELETE /api/tags: {}", e.getMessage());
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_NOT_FOUND, "Not Found", e.getMessage(), logger);
            } catch (OperationForbiddenException e) {
                logger.warn("Operation forbidden in DELETE /api/tags: {}", e.getMessage());
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", e.getMessage(), logger); // 或 403/409
            } catch (OperationFailedException e) {
                logger.error("Operation failed in DELETE /api/tags: {}", e.getMessage(), e.getCause());
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", e.getMessage(), logger);
            } catch (Exception e) {
                logger.error("Unexpected error in DELETE /api/tags: {}", e.getMessage(), e);
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", "删除标签时发生意外错误。", logger);
            }
        } else {
            logger.warn("Invalid path for DELETE request: /api/tags{}", pathInfo);
            ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method Not Allowed", "DELETE请求需要指定标签ID。", logger);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        logger.info("TagServlet destroyed.");
    }
}
