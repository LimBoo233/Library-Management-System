package com.ILoveU.servlet;

import com.ILoveU.dto.AuthorDTO;
import com.ILoveU.dto.PageDTO;
import com.ILoveU.exception.*;
import com.ILoveU.service.AuthorService;
import com.ILoveU.service.Impl.AuthorServiceImpl;
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


@WebServlet("/api/authors/*")
public class AuthorServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(AuthorServlet.class);
    private AuthorService authorService;

    @Override
    public void init() throws ServletException {
        super.init();
        this.authorService = new AuthorServiceImpl();
        logger.info("AuthorServlet initialized.");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String pathInfo = request.getPathInfo();

        try {
            // 情况1: GET /api/authors (获取作者列表，支持分页和关键词搜索)
            if (pathInfo == null || pathInfo.equals("/")) {
                String pageStr = request.getParameter("page");
                String pageSizeStr = request.getParameter("size"); // API规范中是size
                String searchKeyword = request.getParameter("search");

                int page = (pageStr != null && !pageStr.isEmpty()) ? Integer.parseInt(pageStr) : 1;
                int pageSize = (pageSizeStr != null && !pageSizeStr.isEmpty()) ? Integer.parseInt(pageSizeStr) : 10;

                logger.info("Handling GET /api/authors - keyword: '{}', page: {}, pageSize: {}", searchKeyword, page, pageSize);
                PageDTO<AuthorDTO> pageResult = authorService.getAuthors(searchKeyword, page, pageSize);
                ServletUtil.sendSuccessResponse(response, HttpServletResponse.SC_OK, pageResult);

                // 情况2: GET /api/authors/{authorId} (获取指定作者)
            } else {
                String authorIdStr = pathInfo.substring(1);
                try {
                    int authorId = Integer.parseInt(authorIdStr);
                    logger.info("Handling GET /api/authors/{}", authorId);
                    AuthorDTO authorDTO = authorService.getAuthorById(authorId);
                    ServletUtil.sendSuccessResponse(response, HttpServletResponse.SC_OK, authorDTO);
                } catch (NumberFormatException e) {
                    logger.warn("无效的作者ID格式: {}", authorIdStr, e);
                    ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", "作者ID格式无效。", logger);
                }
            }
        } catch (ValidationException e) {
            logger.warn("Validation error in GET /api/authors: {}", e.getMessage());
            ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", e.getMessage(), e.getErrors(), logger);
        } catch (ResourceNotFoundException e) {
            logger.warn("Resource not found in GET /api/authors: {}", e.getMessage());
            ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_NOT_FOUND, "Not Found", e.getMessage(), logger);
        } catch (OperationFailedException e) {
            logger.error("Operation failed in GET /api/authors: {}", e.getMessage(), e.getCause());
            ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", e.getMessage(), logger);
        } catch (NumberFormatException e) {
            logger.warn("无效的分页参数格式: {}", e.getMessage());
            ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", "分页参数格式无效。", logger);
        } catch (Exception e) {
            logger.error("Unexpected error in GET /api/authors: {}", e.getMessage(), e);
            ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", "获取作者信息时发生意外错误。", logger);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String pathInfo = request.getPathInfo();

        if (pathInfo == null || pathInfo.equals("/")) {
            try {
                JsonObject jsonRequest = ServletUtil.getJsonFromRequestBody(request);
                if (jsonRequest == null) {
                    throw new ValidationException("请求体不能为空。");
                }
                String firstName = jsonRequest.has("firstName") ? jsonRequest.get("firstName").getAsString() : null;
                String lastName = jsonRequest.has("lastName") ? jsonRequest.get("lastName").getAsString() : null;
                String bio = jsonRequest.has("bio") ? jsonRequest.get("bio").getAsString() : null;

                AuthorDTO authorToCreate = new AuthorDTO(null, firstName, lastName, bio, null, null);

                logger.info("Handling POST /api/authors");
                AuthorDTO createdAuthor = authorService.createAuthor(authorToCreate);
                ServletUtil.sendSuccessResponse(response, HttpServletResponse.SC_CREATED, createdAuthor);

            } catch (ValidationException e) {
                logger.warn("Validation error in POST /api/authors: {}", e.getMessage());
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", e.getMessage(), e.getErrors(), logger);
            } catch (DuplicateResourceException e) {
                logger.warn("Duplicate resource error in POST /api/authors: {}", e.getMessage());
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", e.getMessage(), logger);
            } catch (OperationFailedException e) {
                logger.error("Operation failed in POST /api/authors: {}", e.getMessage(), e.getCause());
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", e.getMessage(), logger);
            } catch (JsonSyntaxException e) {
                logger.error("JSON Syntax Error in POST /api/authors: {}", e.getMessage(), e);
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", "请求的JSON格式无效: " + e.getMessage(), logger);
            } catch (IOException e) {
                logger.error("IOException in POST /api/authors: {}", e.getMessage(), e);
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", "读取请求数据时发生错误。", logger);
            } catch (Exception e) {
                logger.error("Unexpected error in POST /api/authors: {}", e.getMessage(), e);
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", "创建作者时发生意外错误。", logger);
            }
        } else {
            logger.warn("Invalid path for POST request: /api/authors{}", pathInfo);
            ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method Not Allowed", "此路径不支持POST请求。", logger);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String pathInfo = request.getPathInfo();

        if (pathInfo != null && pathInfo.matches("/\\d+")) {
            String authorIdStr = pathInfo.substring(1);
            try {
                int authorId = Integer.parseInt(authorIdStr);
                JsonObject jsonRequest = ServletUtil.getJsonFromRequestBody(request);
                if (jsonRequest == null) {
                    throw new ValidationException("请求体不能为空。");
                }
                String firstName = jsonRequest.has("firstName") ? jsonRequest.get("firstName").getAsString() : null;
                String lastName = jsonRequest.has("lastName") ? jsonRequest.get("lastName").getAsString() : null;
                String bio = jsonRequest.has("bio") ? jsonRequest.get("bio").getAsString() : null;
                AuthorDTO authorToUpdate = new AuthorDTO(authorId, firstName, lastName, bio, null, null);

                logger.info("Handling PUT /api/authors/{}", authorId);
                AuthorDTO updatedAuthor = authorService.updateAuthor(authorId, authorToUpdate);
                ServletUtil.sendSuccessResponse(response, HttpServletResponse.SC_OK, updatedAuthor);

            } catch (NumberFormatException e) {
                logger.warn("无效的作者ID格式: {}", authorIdStr, e);
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", "作者ID格式无效。", logger);
            } catch (ValidationException e) {
                logger.warn("Validation error in PUT /api/authors: {}", e.getMessage());
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", e.getMessage(), e.getErrors(), logger);
            } catch (ResourceNotFoundException e) {
                logger.warn("Resource not found in PUT /api/authors: {}", e.getMessage());
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_NOT_FOUND, "Not Found", e.getMessage(), logger);
            } catch (DuplicateResourceException e) {
                logger.warn("Duplicate resource error in PUT /api/authors: {}", e.getMessage());
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", e.getMessage(), logger);
            } catch (OperationFailedException e) {
                logger.error("Operation failed in PUT /api/authors: {}", e.getMessage(), e.getCause());
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", e.getMessage(), logger);
            } catch (JsonSyntaxException e) {
                logger.error("JSON Syntax Error in PUT /api/authors: {}", e.getMessage(), e);
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", "请求的JSON格式无效: " + e.getMessage(), logger);
            } catch (IOException e) {
                logger.error("IOException in PUT /api/authors: {}", e.getMessage(), e);
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", "读取请求数据时发生错误。", logger);
            } catch (Exception e) {
                logger.error("Unexpected error in PUT /api/authors: {}", e.getMessage(), e);
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", "更新作者时发生意外错误。", logger);
            }
        } else {
            logger.warn("Invalid path for PUT request: /api/authors{}", pathInfo);
            ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method Not Allowed", "PUT请求需要指定作者ID。", logger);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setCharacterEncoding("UTF-8");

        String pathInfo = request.getPathInfo();

        if (pathInfo != null && pathInfo.matches("/\\d+")) {
            String authorIdStr = pathInfo.substring(1);
            try {
                int authorId = Integer.parseInt(authorIdStr);
                logger.info("Handling DELETE /api/authors/{}", authorId);
                authorService.deleteAuthor(authorId);
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);

            } catch (NumberFormatException e) {
                logger.warn("无效的作者ID格式: {}", authorIdStr, e);
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", "作者ID格式无效。", logger);
            } catch (ResourceNotFoundException e) {
                logger.warn("Resource not found in DELETE /api/authors: {}", e.getMessage());
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_NOT_FOUND, "Not Found", e.getMessage(), logger);
            } catch (OperationForbiddenException e) {
                logger.warn("Operation forbidden in DELETE /api/authors: {}", e.getMessage());
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", e.getMessage(), logger);
            } catch (OperationFailedException e) {
                logger.error("Operation failed in DELETE /api/authors: {}", e.getMessage(), e.getCause());
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", e.getMessage(), logger);
            } catch (Exception e) {
                logger.error("Unexpected error in DELETE /api/authors: {}", e.getMessage(), e);
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", "删除作者时发生意外错误。", logger);
            }
        } else {
            logger.warn("Invalid path for DELETE request: /api/authors{}", pathInfo);
            ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method Not Allowed", "DELETE请求需要指定作者ID。", logger);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        logger.info("AuthorServlet destroyed.");
    }
}
