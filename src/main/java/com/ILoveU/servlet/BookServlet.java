package com.ILoveU.servlet;

import com.ILoveU.dto.*;        // 导入所有DTO
import com.ILoveU.exception.*;  // 导入所有自定义异常
import com.ILoveU.dao.*;      // 导入所有DAO接口 (Service实现类会用到)
import com.ILoveU.dao.impl.*;      // 导入所有DAO接口 (Service实现类会用到)
import com.ILoveU.model.*;    // 导入所有模型实体 (Service实现类会用到)
import com.ILoveU.service.*;
import com.ILoveU.service.Impl.*;// 导入所有Service接口
import com.ILoveU.util.ServletUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

// 使用 jakarta.servlet.* 因为你用的是Tomcat 10
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
// import jakarta.servlet.http.HttpSession; // 如果需要获取用户信息进行日志记录或细致权限控制

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet("/api/books/*")
public class BookServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(BookServlet.class);
    private BookService bookService;

    @Override
    public void init() throws ServletException {
        super.init();

        BookDAO bookDAO = new BookDAOImpl();
        AuthorDAO authorDAO = new AuthorDAOImpl();
        PressDAO pressDAO = new PressDAOImpl();
        TagDAO tagDAO = new TagDAOImpl();
        LoanDAO loanDAO = new LoanDAOImpl();

        this.bookService = new BookServiceImpl(bookDAO, authorDAO, pressDAO, tagDAO, loanDAO);
        logger.info("BookServlet initialized.");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String pathInfo = request.getPathInfo();
        try {
            // 情况1: GET /api/books (获取图书列表，支持分页、搜索、按出版社/标签过滤)
            if (pathInfo == null || pathInfo.equals("/")) {
                // 从请求参数中获取过滤和分页参数
                String searchKeyword = request.getParameter("search");
                String pressIdStr = request.getParameter("press");
                String tagIdStr = request.getParameter("tag");
                String pageStr = request.getParameter("page");
                String pageSizeStr = request.getParameter("size");

                // API规范中分页参数默认值: page=1, size=10
                int page = (pageStr != null && !pageStr.isEmpty()) ? Integer.parseInt(pageStr) : 1;
                int pageSize = (pageSizeStr != null && !pageSizeStr.isEmpty()) ? Integer.parseInt(pageSizeStr) : 10;
                Integer pressId = (pressIdStr != null && !pressIdStr.isEmpty()) ? Integer.parseInt(pressIdStr) : null;
                Integer tagId = (tagIdStr != null && !tagIdStr.isEmpty()) ? Integer.parseInt(tagIdStr) : null;

                logger.info("Handling GET /api/books - keyword: '{}', pressId: {}, tagId: {}, page: {}, pageSize: {}",
                        searchKeyword, pressId, tagId, page, pageSize);

                PageDTO<BookDTO> pageResult = bookService.getBooks(searchKeyword, pressId, tagId, page, pageSize);
                ServletUtil.sendSuccessResponse(response, HttpServletResponse.SC_OK, pageResult);

            // 情况2: GET /api/books/{bookId} (获取指定图书详情)
            } else {
                String bookIdStr = pathInfo.substring(1); // 移除开头的 '/'
                try {
                    int bookId = Integer.parseInt(bookIdStr);
                    logger.info("Handling GET /api/books/{}", bookId);
                    BookDTO bookDTO = bookService.getBookById(bookId);
                    ServletUtil.sendSuccessResponse(response, HttpServletResponse.SC_OK, bookDTO);
                } catch (NumberFormatException e) {
                    logger.warn("无效的图书ID格式: {}", bookIdStr, e);
                    ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", "图书ID格式无效。", logger);
                }
            }
        } catch (ValidationException e) {
            logger.warn("Validation error in GET /api/books: {}", e.getMessage());
            ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", e.getMessage(), e.getErrors(), logger);
        } catch (ResourceNotFoundException e) {
            logger.warn("Resource not found in GET /api/books: {}", e.getMessage());
            ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_NOT_FOUND, "Not Found", e.getMessage(), logger);
        } catch (OperationFailedException e) {
            logger.error("Operation failed in GET /api/books: {}", e.getMessage(), e.getCause());
            ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", e.getMessage(), logger);
        } catch (NumberFormatException e) { // 捕获分页或ID参数转换的异常
            logger.warn("无效的数字参数格式: {}", e.getMessage());
            ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", "请求参数中的数字格式无效。", logger);
        } catch (Exception e) {
            logger.error("Unexpected error in GET /api/books: {}", e.getMessage(), e);
            ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", "获取图书信息时发生意外错误。", logger);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String pathInfo = request.getPathInfo();

        // POST /api/books (创建新图书)
        if (pathInfo == null || pathInfo.equals("/")) {
            try {
                JsonObject jsonRequest = ServletUtil.getJsonFromRequestBody(request);
                if (jsonRequest == null) {
                    throw new ValidationException("请求体不能为空。");
                }

                // 根据API规范（第3页）解析请求体
                String title = jsonRequest.has("title") ? jsonRequest.get("title").getAsString() : null;
                String isbn = jsonRequest.has("isbn") ? jsonRequest.get("isbn").getAsString() : null;
                Integer numCopiesAvailable = jsonRequest.has("numCopiesAvailable") && !jsonRequest.get("numCopiesAvailable").isJsonNull()
                                            ? jsonRequest.get("numCopiesAvailable").getAsInt() : null;
                Integer pressId = jsonRequest.has("pressId") && !jsonRequest.get("pressId").isJsonNull()
                                  ? jsonRequest.get("pressId").getAsInt() : null;

                List<Integer> authorIds = new ArrayList<>();
                if (jsonRequest.has("authorIds") && jsonRequest.get("authorIds").isJsonArray()) {
                    jsonRequest.get("authorIds").getAsJsonArray().forEach(idEl -> authorIds.add(idEl.getAsInt()));
                }

                List<Integer> tagIds = new ArrayList<>();
                if (jsonRequest.has("tagIds") && jsonRequest.get("tagIds").isJsonArray()) {
                    jsonRequest.get("tagIds").getAsJsonArray().forEach(idEl -> tagIds.add(idEl.getAsInt()));
                }

                // 使用BookCreateRequestDTO（或直接构造一个临时的，如果不想创建该类）
                BookCreateRequestDTO createRequest = new BookCreateRequestDTO(title, isbn, numCopiesAvailable, authorIds, pressId, tagIds);
                // 如果BookCreateRequestDTO没有构造函数，则需要手动设置字段

                logger.info("Handling POST /api/books with title: {}", title);
                BookDTO createdBook = bookService.createBook(createRequest);
                ServletUtil.sendSuccessResponse(response, HttpServletResponse.SC_CREATED, createdBook);

            } catch (ValidationException e) {
                logger.warn("Validation error in POST /api/books: {}", e.getMessage());
                e.printStackTrace();
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", e.getMessage(), e.getErrors(), logger);
            } catch (ResourceNotFoundException e) { // 例如，提供的pressId, authorId, tagId无效
                logger.warn("Resource not found during POST /api/books: {}", e.getMessage());
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", e.getMessage(), logger); // 通常关联ID无效是400
            } catch (DuplicateResourceException e) { // 例如，ISBN重复
                logger.warn("Duplicate resource error in POST /api/books: {}", e.getMessage());
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", e.getMessage(), logger);
            } catch (OperationFailedException e) {
                logger.error("Operation failed in POST /api/books: {}", e.getMessage(), e.getCause());
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", e.getMessage(), logger);
            } catch (JsonSyntaxException e) {
                logger.error("JSON Syntax Error in POST /api/books: {}", e.getMessage(), e);
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", "请求的JSON格式无效: " + e.getMessage(), logger);
            } catch (IOException e) {
                logger.error("IOException in POST /api/books: {}", e.getMessage(), e);
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", "读取请求数据时发生错误。", logger);
            } catch (Exception e) {
                logger.error("Unexpected error in POST /api/books: {}", e.getMessage(), e);
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", "创建图书时发生意外错误。", logger);
            }
        } else {
            logger.warn("Invalid path for POST request: /api/books{}", pathInfo);
            ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method Not Allowed", "此路径不支持POST请求。", logger);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String pathInfo = request.getPathInfo();

        // PUT /api/books/{bookId} (更新图书)
        if (pathInfo != null && pathInfo.matches("/\\d+")) { // 匹配 /数字
            String bookIdStr = pathInfo.substring(1);
            try {
                int bookId = Integer.parseInt(bookIdStr);
                JsonObject jsonRequest = ServletUtil.getJsonFromRequestBody(request);
                if (jsonRequest == null) {
                    throw new ValidationException("请求体不能为空。");
                }

                // API规范说更新时请求体与POST相同，所有可更新字段必填
                String title = jsonRequest.has("title") ? jsonRequest.get("title").getAsString() : null;
                String isbn = jsonRequest.has("isbn") ? jsonRequest.get("isbn").getAsString() : null;
                Integer numCopiesAvailable = jsonRequest.has("numCopiesAvailable") && !jsonRequest.get("numCopiesAvailable").isJsonNull()
                                            ? jsonRequest.get("numCopiesAvailable").getAsInt() : null;
                Integer pressId = jsonRequest.has("pressId") && !jsonRequest.get("pressId").isJsonNull()
                                  ? jsonRequest.get("pressId").getAsInt() : null;
                List<Integer> authorIds = new ArrayList<>();
                if (jsonRequest.has("authorIds") && jsonRequest.get("authorIds").isJsonArray()) {
                    jsonRequest.get("authorIds").getAsJsonArray().forEach(idEl -> authorIds.add(idEl.getAsInt()));
                }
                List<Integer> tagIds = new ArrayList<>();
                if (jsonRequest.has("tagIds") && jsonRequest.get("tagIds").isJsonArray()) {
                    jsonRequest.get("tagIds").getAsJsonArray().forEach(idEl -> tagIds.add(idEl.getAsInt()));
                }

                BookCreateRequestDTO updateRequestDTO = new BookCreateRequestDTO(title, isbn, numCopiesAvailable, authorIds, pressId, tagIds);

                logger.info("Handling PUT /api/books/{}", bookId);
                BookDTO updatedBook = bookService.updateBook(bookId, updateRequestDTO);
                ServletUtil.sendSuccessResponse(response, HttpServletResponse.SC_OK, updatedBook);

            } catch (NumberFormatException e) {
                logger.warn("无效的图书ID格式: {}", bookIdStr, e);
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", "图书ID格式无效。", logger);
            } catch (ValidationException e) {
                logger.warn("Validation error in PUT /api/books: {}", e.getMessage());
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", e.getMessage(), e.getErrors(), logger);
            } catch (ResourceNotFoundException e) {
                logger.warn("Resource not found in PUT /api/books: {}", e.getMessage());
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_NOT_FOUND, "Not Found", e.getMessage(), logger);
            } catch (DuplicateResourceException e) {
                logger.warn("Duplicate resource error in PUT /api/books: {}", e.getMessage());
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", e.getMessage(), logger);
            } catch (OperationFailedException e) {
                logger.error("Operation failed in PUT /api/books: {}", e.getMessage(), e.getCause());
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", e.getMessage(), logger);
            } catch (JsonSyntaxException e) {
                logger.error("JSON Syntax Error in PUT /api/books: {}", e.getMessage(), e);
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", "请求的JSON格式无效: " + e.getMessage(), logger);
            } catch (IOException e) {
                logger.error("IOException in PUT /api/books: {}", e.getMessage(), e);
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", "读取请求数据时发生错误。", logger);
            } catch (Exception e) {
                logger.error("Unexpected error in PUT /api/books: {}", e.getMessage(), e);
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", "更新图书时发生意外错误。", logger);
            }
        } else {
            logger.warn("Invalid path for PUT request: /api/books{}", pathInfo);
            ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method Not Allowed", "PUT请求需要指定图书ID。", logger);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setCharacterEncoding("UTF-8");
        String pathInfo = request.getPathInfo();

        // DELETE /api/books/{bookId} (删除图书)
        if (pathInfo != null && pathInfo.matches("/\\d+")) {
            String bookIdStr = pathInfo.substring(1);
            try {
                int bookId = Integer.parseInt(bookIdStr);
                logger.info("Handling DELETE /api/books/{}", bookId);
                bookService.deleteBook(bookId);
                ServletUtil.sendSuccessResponse(response, HttpServletResponse.SC_NO_CONTENT, null); // 204 No Content

            } catch (NumberFormatException e) {
                logger.warn("无效的图书ID格式: {}", bookIdStr, e);
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", "图书ID格式无效。", logger);
            } catch (ResourceNotFoundException e) {
                logger.warn("Resource not found in DELETE /api/books: {}", e.getMessage());
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_NOT_FOUND, "Not Found", e.getMessage(), logger);
            } catch (OperationForbiddenException e) { // 例如，图书有在借记录
                logger.warn("Operation forbidden in DELETE /api/books: {}", e.getMessage());
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_BAD_REQUEST, "Bad Request", e.getMessage(), logger); // API规范中删除失败是400
            } catch (OperationFailedException e) {
                logger.error("Operation failed in DELETE /api/books: {}", e.getMessage(), e.getCause());
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", e.getMessage(), logger);
            } catch (Exception e) {
                logger.error("Unexpected error in DELETE /api/books: {}", e.getMessage(), e);
                ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", "删除图书时发生意外错误。", logger);
            }
        } else {
            logger.warn("Invalid path for DELETE request: /api/books{}", pathInfo);
            ServletUtil.sendErrorResponse(response, request, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method Not Allowed", "DELETE请求需要指定图书ID。", logger);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        logger.info("BookServlet destroyed.");
    }
}
