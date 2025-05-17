package com.ILoveU.service;

import com.ILoveU.dto.LoanDTO;
import com.ILoveU.dto.PageDTO;
import com.ILoveU.exception.OperationFailedException;
import com.ILoveU.exception.OperationForbiddenException;
import com.ILoveU.exception.ResourceNotFoundException;
import com.ILoveU.exception.ValidationException;

/**
 * LoanService 接口定义了与图书借阅和归还相关的业务逻辑操作。
 * 它使用DTO（Data Transfer Objects）与外部层进行数据交换，
 * 并通过抛出自定义业务异常来指示操作中的问题。
 */
public interface LoanService {

    /**
     * 处理用户借阅图书的请求。
     * 此方法会校验用户、图书的有效性，检查图书库存，
     * 创建借阅记录，并更新图书库存。
     *
     * @param userId 借阅用户的ID。
     * @param bookId 要借阅的图书的ID。
     * @return 创建成功的 {@link LoanDTO} 对象，包含借阅详情、应归还日期和逾期状态。
     * @throws ResourceNotFoundException   如果用户或图书未找到。
     * @throws ValidationException         如果输入参数无效（例如，用户ID或图书ID无效）。
     * @throws OperationForbiddenException 如果图书无可用库存，或用户有其他借阅限制。
     * @throws OperationFailedException    如果由于数据库或其他原因导致借阅操作失败。
     */
    LoanDTO checkoutBook(int userId, int bookId)
            throws ResourceNotFoundException, ValidationException, OperationForbiddenException, OperationFailedException;

    /**
     * 处理用户归还图书的请求。
     * 此方法会校验借阅记录的有效性，更新归还日期，并更新图书库存。
     *
     * @param loanId 要归还的借阅记录的ID。
     * @return 更新成功后的 {@link LoanDTO} 对象，包含归还日期和更新后的逾期状态。
     * @throws ResourceNotFoundException 如果借阅记录未找到。
     * @throws ValidationException       如果输入参数无效或该书已归还。
     * @throws OperationFailedException  如果由于数据库或其他原因导致归还操作失败。
     */
    LoanDTO returnBook(int loanId)
            throws ResourceNotFoundException, ValidationException, OperationFailedException;

    /**
     * 根据ID获取借阅记录的详细信息。
     *
     * @param loanId 借阅记录的ID。
     * @return 找到的 {@link LoanDTO} 对象。
     * @throws ResourceNotFoundException 如果借阅记录未找到。
     * @throws OperationFailedException  如果查询过程中发生错误。
     */
    LoanDTO getLoanById(int loanId) throws ResourceNotFoundException, OperationFailedException;


    /**
     * (可选API功能) 分页获取指定用户的借阅记录列表。
     *
     * @param userId   用户的ID。
     * @param page     请求的页码 (通常从1开始计数)。
     * @param pageSize 每页期望返回的记录数。
     * @return 一个 {@link PageDTO} 对象，其中包含当前页的借阅记录列表 ({@link java.util.List}<{@link LoanDTO}>)
     * 以及分页元数据。
     * @throws ResourceNotFoundException 如果用户未找到。
     * @throws ValidationException       如果分页参数无效。
     * @throws OperationFailedException  如果在获取数据过程中发生意外的后端错误。
     */
    PageDTO<LoanDTO> getLoansByUserId(int userId, int page, int pageSize)
            throws ResourceNotFoundException, ValidationException, OperationFailedException;
}