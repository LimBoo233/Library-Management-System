package com.ILoveU.service.Impl;

import com.ILoveU.dao.*;
import com.ILoveU.dto.ApiErrorResponse;
import com.ILoveU.dto.BookCreateRequestDTO;
import com.ILoveU.dto.BookDTO;
import com.ILoveU.dto.PageDTO;
import com.ILoveU.exception.*;
import com.ILoveU.model.Author;
import com.ILoveU.model.Book;
import com.ILoveU.model.Press;
import com.ILoveU.model.Tag;
import com.ILoveU.service.BookService;
import com.ILoveU.servlet.BookServlet;
import com.ILoveU.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class BookServiceImpl implements BookService {

    private static final Logger logger = LoggerFactory.getLogger(BookServiceImpl.class);

    private final BookDAO bookDAO;
    private final AuthorDAO authorDAO;
    private final PressDAO pressDAO;
    private final TagDAO tagDAO;
    private final LoanDAO loanDAO; // 用于 deleteBook 操作

    // 通过构造函数注入DAO实例
    public BookServiceImpl(BookDAO bookDAO, AuthorDAO authorDAO, PressDAO pressDAO, TagDAO tagDAO, LoanDAO loanDAO) {
        this.bookDAO = bookDAO;
        this.authorDAO = authorDAO;
        this.pressDAO = pressDAO;
        this.tagDAO = tagDAO;
        this.loanDAO = loanDAO;
    }
    /**
     * 辅助方法，将 Book 实体转换为 BookDTO。
     * 这包括将关联的实体转换为它们各自的摘要DTO。
     * @param book 要转换的 Book 实体
     * @return 转换后的 BookDTO 对象，如果输入为null则返回null
     */
    private BookDTO convertToBookDTO(Book book) {
        if (book == null) {
            return null;
        }

        BookDTO dto = new BookDTO();
        dto.setId(book.getBookId());
        dto.setTitle(book.getTitle());
        dto.setIsbn(book.getIsbn());
        dto.setNumCopiesTotal(book.getNumCopiesTotal());
        dto.setNumCopiesAvailable(book.getNumCopiesAvailable());

        // 使用DateUtil将Timestamp格式化为ISO8601字符串
        dto.setCreatedAt(book.getCreatedAt() != null ? DateUtil.formatTimestampToISOString(book.getCreatedAt()) : null);
        dto.setUpdatedAt(book.getUpdatedAt() != null ? DateUtil.formatTimestampToISOString(book.getUpdatedAt()) : null);

        // 映射出版社信息
        if (book.getPress() != null) {
            dto.setPress(new BookDTO.PressInfoDTO(book.getPress().getPressId(), book.getPress().getName()));
        }

        // 映射作者列表信息
        if (book.getAuthors() != null) {
            dto.setAuthors(book.getAuthors().stream()
                    .map(author -> new BookDTO.AuthorInfoDTO(author.getAuthorId(), author.getFirstName(), author.getLastName()))
                    .collect(Collectors.toList()));
        } else {
            dto.setAuthors(Collections.emptyList()); // 如果没有作者，则设置为空列表
        }

        // 映射标签列表信息
        if (book.getTags() != null) {
            dto.setTags(book.getTags().stream()
                    .map(tag -> new BookDTO.TagInfoDTO(tag.getTagId(), tag.getName()))
                    .collect(Collectors.toList()));
        } else {
            dto.setTags(Collections.emptyList()); // 如果没有标签，则设置为空列表
        }

        return dto;
    }

    @Override
    public PageDTO<BookDTO> getBooks(String searchKeyword, Integer pressId, Integer tagId, int page, int pageSize)
            throws ValidationException, OperationFailedException {

        // 校验分页参数
        if (page <= 0) {
            throw new ValidationException("页码必须是正整数。", Collections.singletonList(new ApiErrorResponse.FieldErrorDetail("page", "页码必须大于0")));
        }
        if (pageSize <= 0) {
            throw new ValidationException("每页大小必须是正整数。", Collections.singletonList(new ApiErrorResponse.FieldErrorDetail("pageSize", "每页大小必须大于0")));
        }

        // 处理搜索关键词，去除前后空格，如果为空则设为null
        String trimmedKeyword = (searchKeyword != null && !searchKeyword.trim().isEmpty()) ? searchKeyword.trim() : null;

        logger.debug("获取图书分页列表 - 关键词: '{}', 出版社ID: {}, 标签ID: {}, 页码: {}, 每页大小: {}",
                trimmedKeyword, pressId, tagId, page, pageSize);

        List<Book> books;
        long totalBooks;
        try {
            // 调用DAO层获取数据
            books = bookDAO.findBooks(trimmedKeyword, pressId, tagId, page, pageSize);
            totalBooks = bookDAO.countBooks(trimmedKeyword, pressId, tagId);
        } catch (Exception e) {
            logger.error("Service层获取图书列表时发生数据库错误。", e);
            throw new OperationFailedException("获取图书列表失败，请稍后再试。", e);
        }

        // 将实体列表转换为DTO列表
        List<BookDTO> bookDTOs = books.stream()
                .map(this::convertToBookDTO)
                .collect(Collectors.toList());

        return new PageDTO<>(bookDTOs, totalBooks, page, pageSize);
    }

    @Override
    public BookDTO getBookById(int bookId) throws ResourceNotFoundException, OperationFailedException {
        logger.info("查询图书详情，ID: {}", bookId);
        Book book;
        try {
            book = bookDAO.findBookById(bookId);
        } catch (Exception e) {
            logger.error("Service层通过ID {} 查询图书时发生数据库错误。", bookId, e);
            throw new OperationFailedException("查询图书详情失败，请稍后再试。", e);
        }

        if (book == null) {
            logger.warn("未找到图书，ID: {}", bookId);
            throw new ResourceNotFoundException("未找到ID为 " + bookId + " 的图书。");
        }
        return convertToBookDTO(book);
    }

    @Override
    public BookDTO createBook(BookCreateRequestDTO createRequest)
            throws ValidationException, ResourceNotFoundException, DuplicateResourceException, OperationFailedException {

        // 1. 校验请求DTO及其字段
        if (createRequest == null) {
            throw new ValidationException("创建图书的请求数据不能为空。");
        }
        // 调用辅助方法进行详细校验
        validateBookCreateRequest(createRequest);

        String isbn = createRequest.getIsbn().trim();
        logger.info("尝试创建新图书，ISBN: {}", isbn);

        // 2. 检查ISBN唯一性
        try {
            if (bookDAO.existsByIsbn(isbn)) {
                logger.warn("创建图书失败：ISBN '{}' 已存在。", isbn);
                throw new DuplicateResourceException("ISBN '" + isbn + "' 已存在。");
            }
        } catch (Exception e) {
            logger.error("创建图书时检查ISBN唯一性失败。", e);
            throw new OperationFailedException("检查ISBN唯一性时发生错误。", e);
        }

        // 3. 创建新的Book实体
        Book newBook = new Book();
        newBook.setTitle(createRequest.getTitle().trim());
        newBook.setIsbn(isbn);
        // API规范中POST请求体包含numCopiesAvailable。假设创建新书时，这个值同时用于设置总库存和可用库存。
        newBook.setNumCopiesTotal(createRequest.getNumCopiesAvailable());
        newBook.setNumCopiesAvailable(createRequest.getNumCopiesAvailable());
        // publicationYear 不在 BookCreateRequestDTO 中（根据API规范示例），所以此处不设置。
        // 如果需要，应为: newBook.setPublicationYear(createRequest.getPublicationYear());

        // 4. 处理出版社关联
        try {
            Press press = pressDAO.findPressById(createRequest.getPressId());
            if (press == null) {
                throw new ResourceNotFoundException("未找到ID为 " + createRequest.getPressId() + " 的出版社。");
            }
            newBook.setPress(press);
        } catch (ResourceNotFoundException e) {
            throw e; // 重新抛出ResourceNotFoundException
        } catch (Exception e) {
            logger.error("创建图书时查找出版社ID {} 失败。", createRequest.getPressId(), e);
            throw new OperationFailedException("关联出版社信息时发生错误。", e);
        }

        // 5. 处理作者关联
        List<Author> foundAuthors = new ArrayList<>();
        if (createRequest.getAuthorIds() != null && !createRequest.getAuthorIds().isEmpty()) {
            Set<Integer> uniqueAuthorIds = new HashSet<>(createRequest.getAuthorIds()); // 使用Set去重

            try {
                foundAuthors = authorDAO.findAuthorsByIds(uniqueAuthorIds);
            } catch (Exception e) {
                logger.error("创建图书时查找作者ID列表失败。", e);
                throw new OperationFailedException("关联作者信息时发生错误。", e);
            }
            // 校验是否所有请求的作者ID都找到了对应的实体
            if (foundAuthors.size() != uniqueAuthorIds.size()) {
                // 可以更精确地指出哪些ID未找到
                throw new ResourceNotFoundException("一个或多个提供的作者ID无效。");
            }

         
        }

        // 6. 处理标签关联
        List<Tag> foundTags = new ArrayList<>();
        if (createRequest.getTagIds() != null && !createRequest.getTagIds().isEmpty()) {
            Set<Integer> uniqueTagIds = new HashSet<>(createRequest.getTagIds());

            try {
                foundTags = tagDAO.findTagsByIds(uniqueTagIds); // 假设TagDAO有此方法
            } catch (Exception e) {
                logger.error("创建图书时查找标签ID列表失败。", e);
                throw new OperationFailedException("关联标签信息时发生错误。", e);
            }
            if (foundTags.size() != uniqueTagIds.size()) {
                throw new ResourceNotFoundException("一个或多个提供的标签ID无效。");
            }

         
        }

        for (Author author : foundAuthors) {
            newBook.getAuthors().add(author);
        }

        for (Tag tag : foundTags) {
            newBook.getTags().add(tag);
        }

        // 7. 保存Book实体
        Book savedBook;
        try {
            savedBook = bookDAO.addBook(newBook);
            if (savedBook == null || savedBook.getBookId() == null) {
                throw new OperationFailedException("创建图书后未能获取有效的图书信息。");
            }
        } catch (Exception e) {
            logger.error("创建图书 '{}' 时发生数据库错误。", newBook.getTitle(), e);
            throw new OperationFailedException("创建图书时发生数据库错误。", e);
        }

        logger.info("图书 '{}' (ID: {}) 创建成功。", savedBook.getTitle(), savedBook.getBookId());
        return convertToBookDTO(savedBook); // 转换并返回DTO
    }

    /**
     * 辅助方法，用于校验创建图书请求DTO的字段。
     * @param request 要校验的 BookCreateRequestDTO 对象
     * @throws ValidationException 如果校验失败
     */
    private void validateBookCreateRequest(BookCreateRequestDTO request) throws ValidationException {
        List<ApiErrorResponse.FieldErrorDetail> errors = new ArrayList<>();
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            errors.add(new ApiErrorResponse.FieldErrorDetail("title", "图书标题不能为空。"));
        }
        if (request.getIsbn() == null || request.getIsbn().trim().isEmpty()) {
            errors.add(new ApiErrorResponse.FieldErrorDetail("isbn", "ISBN不能为空。"));
        } else if (request.getIsbn().trim().length() != 13 && request.getIsbn().trim().length() != 10) { // 基础的ISBN长度检查
            errors.add(new ApiErrorResponse.FieldErrorDetail("isbn", "ISBN长度应为10或13位。"));
        }
        if (request.getNumCopiesAvailable() == null || request.getNumCopiesAvailable() < 0) {
            errors.add(new ApiErrorResponse.FieldErrorDetail("numCopiesAvailable", "可用库存数量必须大于或等于0。"));
        }
        if (request.getPressId() == null) {
            errors.add(new ApiErrorResponse.FieldErrorDetail("pressId", "出版社ID不能为空。"));
        }
        // authorIds 和 tagIds 可以是空列表，但如果提供了无效内容（例如非数字ID），
        // 则会在后续加载关联实体时失败。更细致的校验可以检查ID格式。
        if (!errors.isEmpty()) {
            throw new ValidationException("创建图书请求数据校验失败。", errors);
        }
    }


    @Override
    public BookDTO updateBook(int bookId, BookCreateRequestDTO updateRequest)
            throws ResourceNotFoundException, ValidationException, DuplicateResourceException, OperationFailedException {

        logger.info("尝试更新图书ID: {}", bookId);
        if (updateRequest == null) {
            throw new ValidationException("更新图书的请求数据不能为空。");
        }
        // 校验更新请求DTO的内容，与创建类似，但上下文是更新
        validateBookUpdateRequest(updateRequest); // 类似的校验辅助方法

        Book existingBook;
        try {
            existingBook = bookDAO.findBookById(bookId);
        } catch (Exception e) {
            logger.error("更新图书ID {} 时查找失败。", bookId, e);
            throw new OperationFailedException("查找待更新图书时发生错误。", e);
        }

        if (existingBook == null) {
            throw new ResourceNotFoundException("未找到ID为 " + bookId + " 的图书，无法更新。");
        }

        // 更新基本属性
        existingBook.setTitle(updateRequest.getTitle().trim());

        // 处理ISBN更新和唯一性检查
        String newIsbn = updateRequest.getIsbn().trim();
        if (!newIsbn.equalsIgnoreCase(existingBook.getIsbn())) { // 只有当ISBN发生变化时才检查唯一性
            try {
                if (bookDAO.existsByIsbnAndNotBookId(newIsbn, bookId)) {
                    logger.warn("更新图书ID {} 失败：新ISBN '{}' 已被其他图书使用。", bookId, newIsbn);
                    throw new DuplicateResourceException("ISBN '" + newIsbn + "' 已被其他图书使用。");
                }
                existingBook.setIsbn(newIsbn);
            } catch (Exception e) {
                logger.error("更新图书ID {} 时检查新ISBN唯一性失败。", bookId, e);
                throw new OperationFailedException("检查新ISBN唯一性时发生错误。", e);
            }
        }

        // API规范中PUT请求体包含numCopiesAvailable。假设这会同时更新总库存和可用库存。
        // 更复杂的业务逻辑可能需要区分处理。
        existingBook.setNumCopiesTotal(updateRequest.getNumCopiesAvailable());
        existingBook.setNumCopiesAvailable(updateRequest.getNumCopiesAvailable());
        // existingBook.setPublicationYear(updateRequest.getPublicationYear()); // 如果可更新且在DTO中

        // 更新出版社关联
        if (updateRequest.getPressId() != null &&
                (existingBook.getPress() == null || !updateRequest.getPressId().equals(existingBook.getPress().getPressId()))) {
            try {
                Press press = pressDAO.findPressById(updateRequest.getPressId());
                if (press == null) {
                    throw new ResourceNotFoundException("更新图书时，未找到ID为 " + updateRequest.getPressId() + " 的出版社。");
                }
                existingBook.setPress(press);
            } catch (ResourceNotFoundException e) { throw e;} // 重新抛出
            catch (Exception e) {
                logger.error("更新图书ID {} 时查找新出版社ID {} 失败。", bookId, updateRequest.getPressId(), e);
                throw new OperationFailedException("更新出版社信息时发生错误。", e);
            }
        }

        // 更新作者关联 (简单策略：先清除现有所有关联，再添加新的)
        // 注意：这会移除所有现有作者，然后添加DTO中指定的作者。
        // 更高级的策略是比较新旧集合的差异，只做必要的增删。
        // 清除时也应使用辅助方法以维护双向关联（如果Author端有books集合）
        new HashSet<>(existingBook.getAuthors()).forEach(existingBook::removeAuthor); // 使用副本进行迭代和移除
        if (updateRequest.getAuthorIds() != null && !updateRequest.getAuthorIds().isEmpty()) {
            Set<Integer> uniqueAuthorIds = new HashSet<>(updateRequest.getAuthorIds());
            List<Author> newAuthors;
            try {
                newAuthors = authorDAO.findAuthorsByIds(uniqueAuthorIds);
            } catch (Exception e) {
                logger.error("更新图书ID {} 时查找作者列表失败。", bookId, e);
                throw new OperationFailedException("更新作者关联时发生错误。", e);
            }
            if (newAuthors.size() != uniqueAuthorIds.size()) {
                throw new ResourceNotFoundException("更新图书时，一个或多个提供的作者ID无效。");
            }

            for (Author author : newAuthors) {
                existingBook.getAuthors().add(author);
            }

        }

        // 更新标签关联 (与作者类似，先清除后添加)
        new HashSet<>(existingBook.getTags()).forEach(existingBook::removeTag); // 使用副本进行迭代和移除
        if (updateRequest.getTagIds() != null && !updateRequest.getTagIds().isEmpty()) {
            Set<Integer> uniqueTagIds = new HashSet<>(updateRequest.getTagIds());
            List<Tag> newTags;
            try {
                newTags = tagDAO.findTagsByIds(uniqueTagIds);
            } catch (Exception e) {
                logger.error("更新图书ID {} 时查找标签列表失败。", bookId, e);
                throw new OperationFailedException("更新标签关联时发生错误。", e);
            }
            if (newTags.size() != uniqueTagIds.size()) {
                throw new ResourceNotFoundException("更新图书时，一个或多个提供的标签ID无效。");
            }

            for (Tag tag : newTags) {
                existingBook.getTags().add(tag);
            }

        }

        // 持久化更改 (updatedAt将由Hibernate自动更新)
        Book updatedBookEntity;
        try {
            updatedBookEntity = bookDAO.updateBook(existingBook);
            if (updatedBookEntity == null) { // DAO的updateBook应返回更新后的受管实体
                throw new OperationFailedException("更新图书后未能获取有效的图书信息。");
            }
        } catch (Exception e) {
            logger.error("更新图书ID {} 到数据库时失败。", bookId, e);
            throw new OperationFailedException("更新图书信息到数据库时发生错误。", e);
        }

        logger.info("图书ID {} 已成功更新。", updatedBookEntity.getBookId());
        return convertToBookDTO(updatedBookEntity);
    }

    /**
     * 辅助方法，用于校验更新图书请求DTO的字段。
     * @param request 要校验的 BookCreateRequestDTO 对象 (复用创建的DTO)
     * @throws ValidationException 如果校验失败
     */
    private void validateBookUpdateRequest(BookCreateRequestDTO request) throws ValidationException {
        // 与创建时的校验类似，因为API规范要求更新时所有可更新字段必填
        List<ApiErrorResponse.FieldErrorDetail> errors = new ArrayList<>();
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            errors.add(new ApiErrorResponse.FieldErrorDetail("title", "图书标题不能为空。"));
        }
        if (request.getIsbn() == null || request.getIsbn().trim().isEmpty()) {
            errors.add(new ApiErrorResponse.FieldErrorDetail("isbn", "ISBN不能为空。"));
        } else if (request.getIsbn().trim().length() != 13 && request.getIsbn().trim().length() != 10) {
            errors.add(new ApiErrorResponse.FieldErrorDetail("isbn", "ISBN长度应为10或13位。"));
        }
        if (request.getNumCopiesAvailable() == null || request.getNumCopiesAvailable() < 0) {
            errors.add(new ApiErrorResponse.FieldErrorDetail("numCopiesAvailable", "可用库存数量必须大于或等于0。"));
        }
        if (request.getPressId() == null) {
            errors.add(new ApiErrorResponse.FieldErrorDetail("pressId", "出版社ID不能为空。"));
        }
        // authorIds 和 tagIds 可以是空列表，表示清除所有作者/标签
        if (!errors.isEmpty()) {
            throw new ValidationException("更新图书请求数据校验失败。", errors);
        }
    }


    @Override
    public void deleteBook(int bookId)
            throws ResourceNotFoundException, OperationForbiddenException, OperationFailedException {
        logger.info("尝试删除图书，ID: {}", bookId);

        Book bookToDelete;
        try {
            bookToDelete = bookDAO.findBookById(bookId);
        } catch (Exception e) {
            logger.error("删除图书ID {} 时查找失败。", bookId, e);
            throw new OperationFailedException("查找待删除图书时发生错误。", e);
        }

        if (bookToDelete == null) {
            throw new ResourceNotFoundException("未找到ID为 " + bookId + " 的图书，无法删除。");
        }

        // 根据API规范 "需无在借记录"，检查是否有活动的借阅记录
        long activeLoanCount;
        try {
            activeLoanCount = loanDAO.countActiveLoansByBookId(bookId); // 依赖LoanDAO
        } catch (Exception e) {
            logger.error("删除图书ID {} 时检查活动借阅记录失败。", bookId, e);
            throw new OperationFailedException("检查图书关联借阅记录时发生错误。", e);
        }

        if (activeLoanCount > 0) {
            logger.warn("删除图书ID {} 失败：该图书尚有 {} 条未归还的借阅记录。", bookId, activeLoanCount);
            throw new OperationForbiddenException("无法删除该图书，尚有 " + activeLoanCount + " 条未归还的借阅记录。");
        }

        // 执行删除
        try {
            if (!bookDAO.deleteBook(bookId)) { // DAO的deleteBook返回boolean
                logger.warn("删除图书ID {} 操作在DAO层未成功执行（可能已被删除或发生未知问题）。", bookId);
                // 如果前面已确认图书存在，而DAO返回false，这可能表示一个不一致的状态
                throw new OperationFailedException("删除图书ID " + bookId + " 操作未成功完成。");
            }
            logger.info("图书ID {} 已成功删除。", bookId);
        } catch (Exception e) { // 例如，如果数据库层面仍有其他约束阻止删除
            logger.error("删除图书ID {} 时发生数据库错误。", bookId, e);
            throw new OperationFailedException("删除图书时发生数据库错误。", e);
        }
    }
}
