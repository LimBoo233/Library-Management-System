package com.ILoveU.dao;

import com.ILoveU.exception.OperationFailedException;
import com.ILoveU.model.Book;

import java.util.List;

/**
 * BookDAO (Data Access Object) 接口定义了与图书（Book）实体相关的数据库操作。
 * 它抽象了数据持久化的具体实现。
 */
public interface BookDAO {

    /**
     * 将一本新的图书对象持久化到数据库。
     *
     * @param book 要添加的 {@link Book} 对象。其ID字段通常应为null或由数据库自动生成。
     * 此对象应已包含所有必需的关联（如Press, Authors, Tags）。
     * @return 持久化后的 {@link Book} 对象，通常包含由数据库生成的ID以及自动填充的时间戳。
     * @throws OperationFailedException 如果添加过程中发生数据库错误。
     */
    Book addBook(Book book) throws OperationFailedException;

    /**
     * 根据指定的ID查找单个图书。
     *
     * @param bookId 要查找的图书的唯一ID。
     * @return 如果找到，则返回对应的 {@link Book} 对象；如果未找到，则返回 {@code null}。
     * @throws OperationFailedException 如果查询过程中发生数据库错误。
     */
    Book findBookById(int bookId) throws OperationFailedException;
    // 备选签名: Optional<Book> findBookById(int bookId) throws OperationFailedException;


    /**
     * 更新数据库中已存在的图书信息。
     * 此方法期望传入的 {@link Book} 对象包含一个有效的ID。
     * 更新操作可能包括图书的基本属性以及其关联的作者和标签集合。
     *
     * @param book 包含更新后信息的 {@link Book} 对象。
     * @return 更新成功后的受Hibernate Session管理的 {@link Book} 对象。
     * @throws OperationFailedException 如果具有给定ID的图书不存在或更新过程中发生数据库错误。
     */
    Book updateBook(Book book) throws OperationFailedException;

    /**
     * 根据指定的ID从数据库中删除一本图书。
     * Service层在调用此方法前，应已处理完所有业务规则检查（例如，图书是否在借）。
     * 删除Book实体时，Hibernate通常会自动处理其在中间连接表（如book_authors, book_tags）中的关联记录。
     *
     * @param bookId 要删除的图书的唯一ID。
     * @return 如果成功删除图书，则返回 {@code true}；
     * 如果未找到具有该ID的图书或删除失败，则返回 {@code false}。
     * @throws OperationFailedException 如果删除过程中发生数据库错误。
     */
    boolean deleteBook(int bookId) throws OperationFailedException;

    /**
     * 根据多种条件分页查询图书列表。
     *
     * @param searchKeyword 可选的搜索关键词。用于匹配图书的标题，或者关联作者的姓名 (不区分大小写)。
     * 如果为null或空，则不根据此关键词过滤。
     * @param pressId       可选的出版社ID。如果提供，则只查询属于该出版社的图书。
     * @param tagId         可选的标签ID。如果提供，则只查询包含该标签的图书。
     * @param page          请求的页码（通常从1开始计数）。
     * @param pageSize      每页期望返回的记录数。
     * @return 包含当前页图书对象的列表 ({@link List}<{@link Book}>)。
     * 如果查询结果为空或发生错误，应返回一个空列表。
     * @throws OperationFailedException 如果查询过程中发生数据库错误。
     */
    List<Book> findBooks(String searchKeyword, Integer pressId, Integer tagId, int page, int pageSize) throws OperationFailedException;

    /**
     * 根据多种条件统计符合条件的图书总数。
     * 用于配合 {@link #findBooks(String, Integer, Integer, int, int)} 方法实现分页。
     *
     * @param searchKeyword 可选的搜索关键词 (同上)。
     * @param pressId       可选的出版社ID (同上)。
     * @param tagId         可选的标签ID (同上)。
     * @return 符合条件的图书总记录数 (long)。
     * @throws OperationFailedException 如果查询过程中发生数据库错误。
     */
    long countBooks(String searchKeyword, Integer pressId, Integer tagId) throws OperationFailedException;

    /**
     * 统计属于指定出版社的图书数量。
     * 用于在删除出版社前检查其是否有关联图书。
     *
     * @param pressId 出版社的ID。
     * @return 该出版社拥有的图书数量。
     * @throws OperationFailedException 如果查询过程中发生数据库错误。
     */
    long countBooksByPressId(int pressId) throws OperationFailedException;

    /**
     * 统计属于指定作者的图书数量。
     * 用于在删除作者前检查其是否有关联图书。
     *
     * @param authorId 作者的ID。
     * @return 该作者撰写的图书数量。
     * @throws OperationFailedException 如果查询过程中发生数据库错误。
     */
    long countBooksByAuthorId(int authorId) throws OperationFailedException;

    /**
     * 统计包含指定标签的图书数量。
     * 用于在删除标签前检查其是否有关联图书。
     *
     * @param tagId 标签的ID。
     * @return 包含该标签的图书数量。
     * @throws OperationFailedException 如果查询过程中发生数据库错误。
     */
    long countBooksByTagId(int tagId) throws OperationFailedException;

    /**
     * 根据ISBN查找图书。ISBN应该是唯一的。
     *
     * @param isbn 要查找的图书的ISBN。
     * @return 如果找到，则返回对应的 {@link Book} 对象；如果未找到，则返回 {@code null}。
     * @throws OperationFailedException 如果查询过程中发生数据库错误。
     */
    Book findBookByIsbn(String isbn) throws OperationFailedException;

    /**
     * 检查指定的ISBN是否已存在于数据库中。
     *
     * @param isbn 要检查的ISBN。
     * @return 如果ISBN已存在，则返回 {@code true}；否则返回 {@code false}。
     * @throws OperationFailedException 如果查询过程中发生数据库错误。
     */
    boolean existsByIsbn(String isbn) throws OperationFailedException;

    /**
     * 检查指定的ISBN是否已被其他图书使用（排除具有特定ID的图书）。
     * 用于在更新图书信息时，验证新的ISBN是否与系统中其他图书的ISBN冲突。
     *
     * @param isbn 要检查的ISBN。
     * @param excludeBookId 要从检查中排除的图书ID (通常是当前正在更新的图书的ID)。
     * @return 如果该ISBN已被系统中除指定ID外的其他图书使用，则返回 {@code true}；否则返回 {@code false}。
     * @throws OperationFailedException 如果查询过程中发生数据库错误。
     */
    boolean existsByIsbnAndNotBookId(String isbn, int excludeBookId) throws OperationFailedException;

}