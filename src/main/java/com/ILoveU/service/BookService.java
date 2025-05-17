package com.ILoveU.service;

import com.ILoveU.dto.BookCreateRequestDTO;
import com.ILoveU.dto.BookDTO;
import com.ILoveU.dto.PageDTO;
import com.ILoveU.exception.*;

/**
 * BookService 接口定义了与图书（Book）相关的业务逻辑操作。
 * 它使用DTO（Data Transfer Objects）与外部层进行数据交换，
 * 并通过抛出自定义业务异常来指示操作中的问题。
 */
public interface BookService {

    /**
     * 根据指定的条件分页获取图书列表。
     * 支持按关键词搜索（书名/作者）、按出版社ID过滤、按标签ID过滤。
     *
     * @param searchKeyword 可选的搜索关键词。如果提供，将用于匹配书名或作者名。
     * @param pressId       可选的出版社ID。如果提供 (非null)，则只查询属于该出版社的图书。
     * @param tagId         可选的标签ID。如果提供 (非null)，则只查询包含该标签的图书。
     * @param page          请求的页码 (通常从1开始计数)。
     * @param pageSize      每页期望返回的记录数。
     * @return 一个 {@link PageDTO} 对象，其中包含当前页的图书列表 ({@link java.util.List}<{@link BookDTO}>)
     * 以及分页元数据。
     * @throws ValidationException 如果页码或每页大小参数无效。
     * @throws OperationFailedException 如果在获取数据过程中发生意外的后端错误。
     */
    PageDTO<BookDTO> getBooks(String searchKeyword, Integer pressId, Integer tagId, int page, int pageSize)
            throws ValidationException, OperationFailedException;

    /**
     * 根据指定的ID获取单个图书的详细信息。
     * 返回的DTO应包含关联的作者、出版社和标签的摘要信息。
     *
     * @param bookId 要查找的图书的唯一ID。
     * @return 找到的 {@link BookDTO} 对象。
     * @throws ResourceNotFoundException 如果未找到具有该ID的图书。
     * @throws OperationFailedException 如果在获取数据过程中发生意外的后端错误。
     */
    BookDTO getBookById(int bookId) throws ResourceNotFoundException, OperationFailedException;

    /**
     * 创建一本新的图书。
     * Service层需要处理：
     * - 数据校验 (例如，标题、ISBN、库存不能为空，ID列表有效性等)。
     * - ISBN的唯一性检查。
     * - 根据传入的 authorIds, pressId, tagIds 查找并关联对应的实体。
     * - 保存Book实体及其关联关系。
     *
     * @param createRequest 包含新图书信息的DTO对象。
     * 这个DTO应该包含如title, isbn, numCopiesAvailable, authorIds (List<Integer>),
     * pressId (Integer), tagIds (List<Integer>) 等字段。
     * (建议为此创建一个专门的 BookCreateRequestDTO 类)
     * @return 创建成功后的 {@link BookDTO} 对象 (应包含所有信息，包括生成的ID和关联对象摘要)。
     * @throws ValidationException 如果输入数据校验失败。
     * @throws ResourceNotFoundException 如果提供的pressId或任何authorId/tagId无效（即找不到对应的实体）。
     * @throws DuplicateResourceException 如果尝试创建的图书ISBN已存在。
     * @throws OperationFailedException 如果由于数据库或其他原因导致创建失败。
     */
    BookDTO createBook(BookCreateRequestDTO createRequest)
            throws ValidationException, ResourceNotFoundException, DuplicateResourceException, OperationFailedException;

    /**
     * 更新一本已存在的图书信息。
     * Service层需要处理：
     * - 确认要更新的图书确实存在。
     * - 数据校验。
     * - 如果ISBN被修改，需要检查新ISBN的唯一性（不能与系统中其他图书的ISBN冲突）。
     * - 更新图书的基本属性。
     * - 更新与作者、标签的关联关系（可能需要移除旧关联，添加新关联）。
     * - 更新出版社关联。
     *
     * @param bookId              要更新的图书的ID。
     * @param bookUpdateRequestDTO 包含要更新的字段信息的DTO对象。
     * (建议为此创建一个专门的 BookUpdateRequestDTO 类)
     * @return 更新成功后的 {@link BookDTO} 对象。
     * @throws ResourceNotFoundException 如果未找到具有该ID的图书，或者提供的pressId/authorId/tagId无效。
     * @throws ValidationException 如果输入数据校验失败。
     * @throws DuplicateResourceException 如果更新后的ISBN与另一个已存在的图书冲突。
     * @throws OperationFailedException 如果由于数据库或其他原因导致更新失败。
     */
    BookDTO updateBook(int bookId, BookCreateRequestDTO bookUpdateRequestDTO)
            throws ResourceNotFoundException, ValidationException, DuplicateResourceException, OperationFailedException;

    /**
     * 根据指定的ID删除一本图书。
     * Service层应负责处理删除操作的业务逻辑，例如：
     * - 检查该图书是否有任何未归还的借阅记录 (根据API规范 "需无在借记录")。
     *
     * @param bookId 要删除的图书的ID。
     * @throws ResourceNotFoundException 如果未找到具有该ID的图书。
     * @throws OperationForbiddenException 如果由于业务规则（例如，图书尚有未归还的借阅记录）不允许删除。
     * @throws OperationFailedException 如果由于数据库或其他原因导致删除失败。
     */
    void deleteBook(int bookId)
            throws ResourceNotFoundException, OperationForbiddenException, OperationFailedException;

}
