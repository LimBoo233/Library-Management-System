package com.ILoveU.dao;

import com.ILoveU.model.Loan;

import java.util.Collections; // 用于返回空列表
import java.util.List;
import java.util.Optional;

/**
 * LoanDAO (Data Access Object) 接口定义了与借阅记录（Loan）实体相关的数据库操作。
 * 方法在发生错误或未找到数据时，通常返回null、空集合或默认值。
 * 具体的错误应由DAO的实现类记录日志。
 */
public interface LoanDAO {

    /**
     * 将一个新的借阅记录对象持久化到数据库。
     * 通常在用户借书时调用。
     *
     * @param loan 要添加的 {@link Loan} 对象。其ID字段通常应为null或由数据库自动生成。
     * @return 持久化后的 {@link Loan} 对象，通常包含由数据库生成的ID。
     * 如果添加过程中发生数据库错误，则返回 {@code null}。
     */
    Loan addLoan(Loan loan);

    /**
     * 根据指定的ID查找单个借阅记录。
     *
     * @param loanId 要查找的借阅记录的唯一ID。
     * @return 如果找到，则返回对应的 {@link Loan} 对象；
     * 如果未找到或查询过程中发生数据库错误，则返回 {@code null}。
     */
    Loan findLoanById(int loanId);
    // 备选签名: Optional<Loan> findLoanById(int loanId);

    /**
     * 更新数据库中已存在的借阅记录信息。
     * 主要用于用户还书时，更新归还日期等信息。
     *
     * @param loan 包含更新后信息的 {@link Loan} 对象。其ID应指向一个已存在的借阅记录。
     * @return 更新成功后的受Hibernate Session管理的 {@link Loan} 对象。
     * 如果具有给定ID的借阅记录不存在或更新过程中发生数据库错误，则返回 {@code null}。
     */
    Loan updateLoan(Loan loan);

    // 通常不提供物理删除借阅记录的方法，因为它们是重要的历史数据。
    // 如果需要，可以考虑软删除或归档。

    /**
     * 统计指定图书当前未归还的（活动的）借阅记录数量。
     * 用于在删除图书前检查其是否仍被借阅。
     *
     * @param bookId 图书的ID。
     * @return 该图书当前未归还的借阅记录数量。如果查询过程中发生数据库错误，则返回 {@code 0L}。
     */
    long countActiveLoansByBookId(int bookId);

    /**
     * (可选API功能) 分页查询指定用户的借阅记录列表。
     *
     * @param userId   用户的ID。
     * @param page     请求的页码。
     * @param pageSize 每页期望返回的记录数。
     * @return 包含当前页指定用户借阅记录对象的列表 ({@link List}<{@link Loan}>)。
     * 如果查询结果为空或发生错误，则返回空列表 ({@link Collections#emptyList()})。
     */
    List<Loan> findLoansByUserId(int userId, int page, int pageSize);

    /**
     * (可选API功能) 统计指定用户的借阅记录总数。
     *
     * @param userId 用户的ID。
     * @return 指定用户的借阅记录总数。如果查询过程中发生数据库错误，则返回 {@code 0L}。
     */
    long countLoansByUserId(int userId);

    /**
     * (可选API功能或管理员功能) 分页查询所有借阅记录列表。
     *
     * @param page     请求的页码。
     * @param pageSize 每页期望返回的记录数。
     * @return 包含当前页所有借阅记录对象的列表 ({@link List}<{@link Loan}>)。
     * 如果查询结果为空或发生错误，则返回空列表 ({@link Collections#emptyList()})。
     */
    List<Loan> findAllLoans(int page, int pageSize);

    /**
     * (可选API功能或管理员功能) 统计所有借阅记录的总数。
     *
     * @return 所有借阅记录的总数。如果查询过程中发生数据库错误，则返回 {@code 0L}。
     */
    long countAllLoans();

}
