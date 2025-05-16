package com.ILoveU.dao;

import com.ILoveU.model.Author;

import java.util.List;

/**
 * AuthorDAO (Data Access Object) 接口定义了与作者（Author）实体相关的数据库操作。
 * 它抽象了数据持久化的具体实现，使得服务层可以独立于特定的数据访问技术。
 */
public interface AuthorDAO {

    /**
     * 分页查询作者列表。
     * 实现时可以考虑添加排序功能，例如按姓氏或ID排序。
     *
     * @param page     请求的页码（通常从1开始计数）。如果小于1，实现时应默认为1。
     * @param pageSize 每页期望返回的记录数。如果小于1，实现时应使用一个合理的默认值。
     * @return 包含当前页作者对象的列表
     * 如果查询结果为空或发生错误，应返回一个空列表，而不是null。
     */
    List<Author> findAuthors(int page, int pageSize);

    /**
     * 根据指定的ID查找单个作者。
     *
     * @param authorId 要查找的作者的唯一ID。
     * @return 如果找到，则返回对应的 {@link Author} 对象；如果未找到具有该ID的作者，则返回 {@code null}。
     */
    Author findAuthorById(int authorId);
    // 备选签名: Optional<Author> findAuthorById(int authorId);

    /**
     * 将一个新的作者对象持久化到数据库。
     *
     * @param author 要添加的 {@link Author} 对象。此对象的ID字段通常应为null或由数据库自动生成。
     * @return 持久化后的 {@link Author} 对象，通常包含由数据库生成的ID。
     * 如果添加失败（例如，由于数据库约束或连接问题），应返回 {@code null} 或抛出运行时异常。
     */
    Author addAuthor(Author author);

    /**
     * 更新数据库中已存在的作者信息。
     * 此方法通常期望传入的 {@link Author} 对象包含一个有效的ID，用于定位要更新的记录。
     *
     * @param author 包含更新后信息的 {@link Author} 对象。其ID应指向一个已存在的作者。
     * @return 更新成功后的 {@link Author} 对象。
     * 如果具有给定ID的作者不存在或更新失败，应返回 {@code null} 或抛出运行时异常。
     */
    Author updateAuthor(Author author);

    /**
     * 根据指定的ID从数据库中删除一个作者。
     * 实现时需要考虑与该作者关联的其他数据（如图书）的处理策略。
     * 通常，如果作者有关联的书籍，直接删除可能会因为外键约束而失败（除非配置了级联删除，但这通常不推荐用于Author和Book之间的关系）。
     * Service层在调用此方法前应处理这些业务规则。
     *
     * @param authorId 要删除的作者的唯一ID。
     * @return 如果成功删除作者，则返回 {@code true}；
     * 如果未找到具有该ID的作者，或者由于其他原因（如外键约束）导致删除失败，则返回 {@code false}。
     * (或者，可以考虑让此方法返回 {@code void}，并在未找到或删除失败时抛出异常)。
     */
    boolean deleteAuthor(int authorId);
    // 备选签名: void deleteAuthor(int authorId) throws AuthorNotFoundException, DataIntegrityViolationException;

    /**
     * 统计数据库中所有作者的总数。
     * 此方法对于实现分页功能非常有用，可以帮助计算总页数。
     *
     * @return 作者的总记录数 (long)。如果发生错误，应返回0或抛出运行时异常。
     */
    long countTotalAuthors();

    /**
     * (可选，但推荐) 检查具有指定姓名组合的作者是否已存在。
     * 这对于在创建新作者之前验证作者是否唯一很有用（尽管作者重名是可能的，
     * 但在某些系统中可能希望避免完全相同的姓名和姓氏组合）。
     *
     * @param firstName 作者的名字。
     * @param lastName  作者的姓氏。
     * @return 如果具有该姓名组合的作者已存在，则返回 {@code true}；否则返回 {@code false}。
     */
    boolean existsByNameIgnoreCase(String firstName, String lastName);

    /**
     * 根据姓名关键词搜索作者 (支持分页)。
     * 对应API规范中图书列表的 search (标题/作者关键词) 功能，作者列表也可能需要类似功能。
     *
     * @param nameKeyword 姓或名的关键词。
     * @param page        页码。
     * @param pageSize    每页大小。
     * @return 符合条件的作者列表。
     */
    List<Author> findAuthorsByNameKeyword(String nameKeyword, int page, int pageSize);

    /**
     * 统计符合姓名关键词搜索条件的作者总数。
     *
     * @param nameKeyword 姓或名的关键词。
     * @return 符合条件的作者总数。
     */
    long countAuthorsByNameKeyword(String nameKeyword);
}