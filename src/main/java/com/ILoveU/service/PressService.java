package com.ILoveU.service;

import com.ILoveU.dto.PageDTO;
import com.ILoveU.dto.PressDTO;

import com.ILoveU.exception.ResourceNotFoundException;
import com.ILoveU.exception.ValidationException;
import com.ILoveU.exception.DuplicateResourceException;
import com.ILoveU.exception.OperationForbiddenException;
import com.ILoveU.exception.OperationFailedException;

import java.util.List;

/**
 * PressService 接口定义了与出版社相关的业务逻辑操作。
 * 它作为Controller层和DAO层之间的桥梁，处理数据校验、业务规则执行，
 * 并使用DTO（Data Transfer Objects）与外部层进行数据交换。
 */
public interface PressService {

    /**
     * 分页获取出版社列表。
     * 返回的出版社信息将是PressDTO格式。
     *
     * @param page     请求的页码 (通常从1开始计数)。
     * @param pageSize 每页期望返回的记录数。
     * @return 一个 {@link PageDTO} 对象，其中包含当前页的出版社列表 ({@link List}<{@link PressDTO}>)
     * 以及分页元数据 (如总记录数、总页数等)。
     * @throws ValidationException 如果页码或每页大小参数无效。
     */
    PageDTO<PressDTO> getPressesWithPagination(int page, int pageSize) throws ValidationException;

    /**
     * 根据指定的ID获取单个出版社的详细信息。
     *
     * @param pressId 要查找的出版社的唯一ID。
     * @return 找到的 {@link PressDTO} 对象。
     * @throws ResourceNotFoundException 如果未找到具有该ID的出版社。
     */
    PressDTO getPressById(int pressId) throws ResourceNotFoundException;

    /**
     * 创建一个新的出版社。
     * Service层应负责在调用DAO之前进行数据校验 (例如，出版社名称不能为空，名称是否已存在等)。
     *
     * @param pressToCreateDTO 包含新出版社信息的DTO对象 (例如，只包含name)。
     * 注意：为了更佳实践，这里的参数类型也应该是DTO，例如 PressCreateRequestDTO。
     * 如果暂时仍使用PressDTO作为输入，请确保其ID字段不被错误使用。
     * @return 创建成功后的 {@link PressDTO} 对象 (应包含由数据库生成的ID和名称)。
     * @throws ValidationException 如果输入数据校验失败。
     * @throws DuplicateResourceException 如果尝试创建的出版社名称已存在。
     * @throws OperationFailedException 如果由于数据库或其他原因导致创建失败。
     */
    PressDTO createNewPress(PressDTO pressToCreateDTO) throws ValidationException, DuplicateResourceException, OperationFailedException;
    // 或者更具体的输入 DTO:
    // PressDTO createNewPress(PressCreateRequestDTO pressCreateRequest) throws ValidationException, DuplicateResourceException, OperationFailedException;


    /**
     * 更新一个已存在的出版社信息。
     * Service层应负责在调用DAO之前进行数据校验，并确认要更新的出版社确实存在。
     *
     * @param pressId 要更新的出版社的ID。
     * @param pressDetailsToUpdateDTO 包含要更新的字段信息的DTO对象 (例如，只包含name)。
     * 注意：为了更佳实践，这里的参数类型也应该是DTO，例如 PressUpdateRequestDTO。
     * @return 更新成功后的 {@link PressDTO} 对象。
     * @throws ResourceNotFoundException 如果未找到具有该ID的出版社。
     * @throws ValidationException 如果输入数据校验失败。
     * @throws DuplicateResourceException 如果更新后的出版社名称与另一个已存在的出版社名称冲突。
     * @throws OperationFailedException 如果由于数据库或其他原因导致更新失败。
     */
    PressDTO updateExistingPress(int pressId, PressDTO pressDetailsToUpdateDTO) throws ResourceNotFoundException, ValidationException, DuplicateResourceException, OperationFailedException;
    // 或者更具体的输入 DTO:
    // PressDTO updateExistingPress(int pressId, PressUpdateRequestDTO pressUpdateRequest) throws ResourceNotFoundException, ValidationException, DuplicateResourceException, OperationFailedException;


    /**
     * 根据指定的ID删除一个出版社。
     * Service层应负责处理删除操作的业务逻辑，例如检查该出版社是否有任何关联的书籍。
     *
     * @param pressId 要删除的出版社的ID。
     * @throws ResourceNotFoundException 如果未找到具有该ID的出版社。
     * @throws OperationForbiddenException 如果由于业务规则（例如，出版社下尚有书籍）不允许删除。
     * @throws OperationFailedException 如果由于数据库或其他原因导致删除失败。
     */
    void deletePressById(int pressId) throws ResourceNotFoundException, OperationForbiddenException, OperationFailedException;
    // 注意：delete方法通常返回void，并通过抛出异常来指示失败。
    // 如果你的API规范要求删除成功时返回特定内容或状态，Controller层可以基于此void方法（即无异常抛出代表成功）来构建响应。
    // 如果坚持返回boolean，也可以：
    // boolean deletePressById(int pressId) throws ResourceNotFoundException, OperationForbiddenException, OperationFailedException;

}