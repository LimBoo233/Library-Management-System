package com.ILoveU.service;

import com.ILoveU.dto.AuthorDTO;
import com.ILoveU.dto.PageDTO;
import com.ILoveU.exception.*;

public interface AuthorService {


    /**
     * 分页获取作者列表，并支持按姓名关键词进行模糊搜索。
     *
     * @param nameKeyword 可选的搜索关键词，用于匹配作者的firstName或lastName (不区分大小写)。
     *                    如果为null或空，则不进行关键词过滤。
     * @param page        请求的页码 (通常从1开始计数)。
     * @param pageSize    每页期望返回的记录数。
     * @return 一个 {@link PageDTO} 对象，其中包含当前页的作者列表 ({@link java.util.List}<{@link AuthorDTO}>)
     * 以及分页元数据 (如总记录数、总页数等)。
     * @throws ValidationException 如果页码或每页大小参数无效。
     */
    PageDTO<AuthorDTO> getAuthors(String nameKeyword, int page, int pageSize) throws ValidationException;

    /**
     * 根据指定的ID获取单个作者的详细信息。
     *
     * @param authorId 要查找的作者的唯一ID。
     * @return 找到的 {@link AuthorDTO} 对象。
     * @throws ResourceNotFoundException 如果未找到具有该ID的作者。
     */
    AuthorDTO getAuthorById(int authorId) throws ResourceNotFoundException;

    /**
     * 创建一个新的作者。
     * Service层应负责在调用DAO之前进行数据校验 (例如，姓名不能为空等)。
     *
     * @param authorDTO 包含新作者信息的DTO对象 (例如，包含firstName, lastName, bio)。
     *                  其ID字段通常应被忽略或为null。
     * @return 创建成功后的 {@link AuthorDTO} 对象 (应包含由数据库生成的ID以及其他信息)。
     * @throws ValidationException        如果输入数据校验失败。
     * @throws DuplicateResourceException 如果尝试创建的作者（例如基于姓名组合）已存在（根据业务规则定义）。
     * @throws OperationFailedException   如果由于数据库或其他原因导致创建失败。
     */
    AuthorDTO createAuthor(AuthorDTO authorDTO) throws ValidationException, DuplicateResourceException, OperationFailedException;

    /**
     * 更新一个已存在的作者信息。
     * Service层应负责在调用DAO之前进行数据校验，并确认要更新的作者确实存在。
     *
     * @param authorId  要更新的作者的ID。
     * @param authorDTO 包含要更新的字段信息的DTO对象。
     * @return 更新成功后的 {@link AuthorDTO} 对象。
     * @throws ResourceNotFoundException  如果未找到具有该ID的作者。
     * @throws ValidationException        如果输入数据校验失败。
     * @throws DuplicateResourceException 如果更新后的作者信息与另一个已存在的作者冲突（根据业务规则定义）。
     * @throws OperationFailedException   如果由于数据库或其他原因导致更新失败。
     */
    AuthorDTO updateAuthor(int authorId, AuthorDTO authorDTO) throws ResourceNotFoundException, ValidationException, DuplicateResourceException, OperationFailedException;

    /**
     * 根据指定的ID删除一个作者。
     * Service层应负责处理删除操作的业务逻辑，例如：
     * - 检查该作者是否有任何关联的书籍。
     * - 根据业务规则决定是否允许删除。
     *
     * @param authorId 要删除的作者的ID。
     * @throws ResourceNotFoundException   如果未找到具有该ID的作者。
     * @throws OperationForbiddenException 如果由于业务规则（例如，作者尚有关联的书籍）不允许删除。
     * @throws OperationFailedException    如果由于数据库或其他原因导致删除失败。
     */
    void deleteAuthor(int authorId) throws ResourceNotFoundException, OperationForbiddenException, OperationFailedException;
}
