package com.ILoveU.service;

import com.ILoveU.dto.PageDTO;
import com.ILoveU.dto.TagDTO;
import com.ILoveU.exception.*;

import java.util.List;

/**
 * TagService 接口定义了与标签（Tag）相关的业务逻辑操作。
 * 它使用DTO（Data Transfer Objects）与外部层进行数据交换，
 * 并通过抛出自定义业务异常来指示操作中的问题。
 */
public interface TagService {

    /**
     * 分页获取标签列表。
     *
     * @param page     请求的页码 (通常从1开始计数)。
     * @param pageSize 每页期望返回的记录数。
     * @return 一个 {@link PageDTO} 对象，其中包含当前页的标签列表 ({@link List}<{@link TagDTO}>)
     * 以及分页元数据 (如总记录数、总页数等)。
     * @throws ValidationException 如果页码或每页大小参数无效。
     * @throws OperationFailedException 如果在获取数据过程中发生意外的后端错误。
     */
    PageDTO<TagDTO> getTags(int page, int pageSize) throws ValidationException, OperationFailedException;

    /**
     * 获取所有标签的列表 (不进行分页)。
     * 这个方法可能用于某些场景，例如在添加或编辑书籍时，提供一个标签选择的下拉列表。
     *
     * @return 包含所有标签对象的列表 ({@link List}<{@link TagDTO}>)；如果没有任何标签，则返回空列表。
     * @throws OperationFailedException 如果在获取数据过程中发生意外的后端错误。
     */
    List<TagDTO> getAllTags() throws OperationFailedException;

    /**
     * 根据指定的ID获取单个标签的详细信息。
     *
     * @param tagId 要查找的标签的唯一ID。
     * @return 找到的 {@link TagDTO} 对象。
     * @throws ResourceNotFoundException 如果未找到具有该ID的标签。
     * @throws OperationFailedException 如果在获取数据过程中发生意外的后端错误。
     */
    TagDTO getTagById(int tagId) throws ResourceNotFoundException, OperationFailedException;

    /**
     * 创建一个新的标签。
     * Service层应负责在调用DAO之前进行数据校验 (例如，标签名称不能为空，名称是否已存在等)。
     *
     * @param tagDTO 包含新标签信息的DTO对象 (通常只包含name，ID由后端生成)。
     * @return 创建成功后的 {@link TagDTO} 对象 (应包含由数据库生成的ID和名称)。
     * @throws ValidationException 如果输入数据校验失败 (例如，名称为空)。
     * @throws DuplicateResourceException 如果尝试创建的标签名称已存在。
     * @throws OperationFailedException 如果由于数据库或其他原因导致创建失败。
     */
    TagDTO createTag(TagDTO tagDTO) throws ValidationException, DuplicateResourceException, OperationFailedException;

    /**
     * 更新一个已存在的标签信息。
     * Service层应负责在调用DAO之前进行数据校验，并确认要更新的标签确实存在。
     *
     * @param tagId   要更新的标签的ID。
     * @param tagDTO  包含要更新的字段信息的DTO对象 (通常只包含name)。
     * @return 更新成功后的 {@link TagDTO} 对象。
     * @throws ResourceNotFoundException 如果未找到具有该ID的标签。
     * @throws ValidationException 如果输入数据校验失败 (例如，新名称为空)。
     * @throws DuplicateResourceException 如果更新后的标签名称与另一个已存在的标签名称冲突。
     * @throws OperationFailedException 如果由于数据库或其他原因导致更新失败。
     */
    TagDTO updateTag(int tagId, TagDTO tagDTO) throws ResourceNotFoundException, ValidationException, DuplicateResourceException, OperationFailedException;

    /**
     * 根据指定的ID删除一个标签。
     * Service层应负责处理删除操作的业务逻辑，例如：
     * - 检查该标签是否有任何关联的书籍。
     * - 根据业务规则决定是否允许删除 (例如，如果有关联书籍，是禁止删除还是解除关联)。
     *
     * @param tagId 要删除的标签的ID。
     * @throws ResourceNotFoundException 如果未找到具有该ID的标签。
     * @throws OperationForbiddenException 如果由于业务规则（例如，标签尚有关联的书籍）不允许删除。
     * @throws OperationFailedException 如果由于数据库或其他原因导致删除失败。
     */
    void deleteTag(int tagId) throws ResourceNotFoundException, OperationForbiddenException, OperationFailedException;

}