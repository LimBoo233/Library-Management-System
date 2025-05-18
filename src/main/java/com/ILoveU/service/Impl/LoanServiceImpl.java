package com.ILoveU.service.Impl;

import com.ILoveU.dao.BookDAO;
import com.ILoveU.dao.LoanDAO;
import com.ILoveU.dao.UserDAO;
import com.ILoveU.dao.impl.BookDAOImpl;
import com.ILoveU.dao.impl.LoanDAOImpl;
import com.ILoveU.dao.impl.UserDAOImpl;
import com.ILoveU.dto.ApiErrorResponse;
import com.ILoveU.dto.LoanDTO;
import com.ILoveU.dto.PageDTO;
import com.ILoveU.exception.OperationFailedException;
import com.ILoveU.exception.OperationForbiddenException;
import com.ILoveU.exception.ResourceNotFoundException;
import com.ILoveU.exception.ValidationException;
import com.ILoveU.model.Book;
import com.ILoveU.model.Loan;
import com.ILoveU.model.User;
import com.ILoveU.service.LoanService;
import com.ILoveU.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class LoanServiceImpl implements LoanService {

    private static final Logger logger = LoggerFactory.getLogger(LoanServiceImpl.class);
    private final LoanDAO loanDAO;
    private final UserDAO userDAO;
    private final BookDAO bookDAO;

    // 默认借阅期限（例如14天）
    private static final int DEFAULT_LOAN_DURATION_DAYS = 14;

    public LoanServiceImpl() {
        this(new LoanDAOImpl(), new UserDAOImpl(), new BookDAOImpl());
    }

    // 通过构造函数注入DAO实例
    public LoanServiceImpl(LoanDAO loanDAO, UserDAO userDAO, BookDAO bookDAO) {
        this.loanDAO = loanDAO;
        this.userDAO = userDAO;
        this.bookDAO = bookDAO;
    }

    /**
     * 将 Loan 实体转换为 LoanDTO，并动态计算 isOverdue 状态。
     * @param loan Loan 实体
     * @return LoanDTO 对象，如果输入为null则返回null
     */
    private LoanDTO convertToLoanDTO(Loan loan) {
        if (loan == null) {
            return null;
        }

        boolean isOverdue;
        Timestamp now = new Timestamp(System.currentTimeMillis());

        if (loan.getReturnDate() != null) {
            // 如果已归还，根据实际归还日期和应归还日期判断是否逾期
            isOverdue = loan.getReturnDate().after(loan.getDueDate());
        } else {
            // 如果未归还，根据当前日期和应归还日期判断是否逾期
            isOverdue = now.after(loan.getDueDate());
        }

        // 确保关联对象不为null，以安全获取ID
        Integer userId = (loan.getUser() != null) ? loan.getUser().getId() : null;
        Integer bookId = (loan.getBook() != null) ? loan.getBook().getBookId() : null;


        return new LoanDTO(
                loan.getLoanId(),
                userId,
                bookId,
                // 使用DateUtil格式化时间戳为ISO8601字符串
                loan.getLoanDate() != null ? DateUtil.formatTimestampToISOString(loan.getLoanDate()) : null,
                loan.getDueDate() != null ? DateUtil.formatTimestampToISOString(loan.getDueDate()) : null,
                loan.getReturnDate() != null ? DateUtil.formatTimestampToISOString(loan.getReturnDate()) : null,
                isOverdue
        );
    }


    @Override
    public LoanDTO checkoutBook(int userId, int bookId)
            throws ResourceNotFoundException, ValidationException, OperationForbiddenException, OperationFailedException {
        logger.info("用户ID: {} 尝试借阅图书ID: {}", userId, bookId);

        // 1. 校验用户是否存在
        User user;
        try {
            user = userDAO.findUserById(userId);
        } catch (Exception e) {
            logger.error("借阅图书时查找用户ID {} 失败。", userId, e);
            throw new OperationFailedException("查找用户信息时发生错误。", e);
        }
        if (user == null) {
            logger.warn("借阅图书失败：未找到用户ID {}", userId);
            throw new ResourceNotFoundException("未找到ID为 " + userId + " 的用户。");
        }

        // 2. 校验图书是否存在
        Book book;
        try {
            book = bookDAO.findBookById(bookId);
        } catch (Exception e) {
            logger.error("借阅图书时查找图书ID {} 失败。", bookId, e);
            throw new OperationFailedException("查找图书信息时发生错误。", e);
        }
        if (book == null) {
            logger.warn("借阅图书失败：未找到图书ID {}", bookId);
            throw new ResourceNotFoundException("未找到ID为 " + bookId + " 的图书。");
        }

        // 3. 检查图书是否有可用库存
        // 注意：由于你们的DDL中有一个BEFORE INSERT触发器来检查库存并减少库存，
        // Service层的这个检查可以看作是一层额外的防护，或者如果触发器逻辑更可靠，这里可以简化。
        // 如果触发器会SIGNAL SQLSTATE '45000'，那么DAO的addLoan方法会抛出异常，这里可以捕获。
        // 为保持Service层业务逻辑的明确性，我们仍然进行一次检查。
        if (book.getNumCopiesAvailable() <= 0) {
            logger.warn("借阅图书ID {} 失败：库存不足。", bookId);
            throw new OperationForbiddenException("图书 '" + book.getTitle() + "' 当前无可用库存。");
        }

        // 4. 创建新的Loan实体
        Loan newLoan = new Loan();
        newLoan.setUser(user);
        newLoan.setBook(book);
        newLoan.setBorrowedBookTitle(book.getTitle()); // 记录借阅时的书名

        Timestamp loanTimestamp = new Timestamp(System.currentTimeMillis());
        newLoan.setLoanDate(loanTimestamp);

        // 计算应归还日期 (例如，借出日期 + 14天)
        Calendar cal = Calendar.getInstance();
        cal.setTime(loanTimestamp);
        cal.add(Calendar.DAY_OF_MONTH, DEFAULT_LOAN_DURATION_DAYS);
        newLoan.setDueDate(new Timestamp(cal.getTimeInMillis()));

        newLoan.setReturnDate(null); // 新借阅，归还日期为null
        // isOverdue 和 createdAt (如果Loan实体有) 会在DTO转换或由Hibernate自动处理

        // 5. 保存借阅记录 (数据库触发器会自动处理库存减少)
        Loan savedLoan;
        try {
            savedLoan = loanDAO.addLoan(newLoan);
            if (savedLoan == null || savedLoan.getLoanId() == null) {
                // 这种情况通常意味着DAO层的save操作因为某些原因（可能是触发器导致的）没有成功返回预期的持久化对象
                logger.error("创建借阅记录后未能获取有效的记录信息。用户ID: {}, 图书ID: {}", userId, bookId);
                throw new OperationFailedException("创建借阅记录失败，未能保存信息。");
            }
        } catch (Exception e) { // 捕获DAO层可能因触发器SIGNAL而抛出的异常
            logger.error("用户ID {} 借阅图书ID {} 时，保存借阅记录失败: {}", userId, bookId, e.getMessage(), e);
            // 检查是否是库存不足的特定错误（如果触发器使用SIGNAL SQLSTATE '45000'）
            if (e.getMessage() != null && e.getMessage().contains("图书无可用库存")) {
                throw new OperationForbiddenException("图书 '" + book.getTitle() + "' 当前无可用库存 (触发器检查)。");
            }
            throw new OperationFailedException("创建借阅记录时发生数据库错误。", e);
        }

        logger.info("用户ID {} 成功借阅图书ID {}，借阅记录ID: {}", userId, bookId, savedLoan.getLoanId());
        return convertToLoanDTO(savedLoan);
    }

    @Override
    public LoanDTO returnBook(int loanId)
            throws ResourceNotFoundException, ValidationException, OperationFailedException {
        logger.info("尝试归还借阅记录ID: {}", loanId);

        // 1. 查找借阅记录
        Loan loanToReturn;
        try {
            loanToReturn = loanDAO.findLoanById(loanId);
        } catch (Exception e) {
            logger.error("归还图书时查找借阅记录ID {} 失败。", loanId, e);
            throw new OperationFailedException("查找借阅记录时发生错误。", e);
        }

        if (loanToReturn == null) {
            logger.warn("归还图书失败：未找到借阅记录ID {}", loanId);
            throw new ResourceNotFoundException("未找到ID为 " + loanId + " 的借阅记录。");
        }

        // 2. 检查是否已归还
        if (loanToReturn.getReturnDate() != null) {
            logger.warn("归还图书失败：借阅记录ID {} 已于 {} 归还。", loanId, DateUtil.formatTimestampToISOString(loanToReturn.getReturnDate()));
            throw new ValidationException("该书已于 " + DateUtil.formatTimestampToISOString(loanToReturn.getReturnDate()) + " 归还。");
        }

        // 3. 设置归还日期
        loanToReturn.setReturnDate(new Timestamp(System.currentTimeMillis()));
        // isOverdue 状态将在 convertToLoanDTO 中根据新的 returnDate 计算
        // 数据库触发器会自动处理库存增加

        // 4. 更新借阅记录
        Loan updatedLoan;
        try {
            updatedLoan = loanDAO.updateLoan(loanToReturn);
            if (updatedLoan == null) {
                logger.error("更新借阅记录ID {} 后未能获取有效的记录信息。", loanId);
                throw new OperationFailedException("更新借阅记录失败，未能保存更改。");
            }
        } catch (Exception e) {
            logger.error("更新借阅记录ID {} 时发生数据库错误。", loanId, e);
            throw new OperationFailedException("更新借阅记录时发生数据库错误。", e);
        }

        logger.info("借阅记录ID {} 已成功归还。", loanId);
        return convertToLoanDTO(updatedLoan);
    }

    @Override
    public LoanDTO getLoanById(int loanId) throws ResourceNotFoundException, OperationFailedException {
        logger.info("查询借阅记录，ID: {}", loanId);
        Loan loan;
        try {
            loan = loanDAO.findLoanById(loanId);
        } catch (Exception e) {
            logger.error("Service层通过ID {} 查询借阅记录时发生数据库错误。", loanId, e);
            throw new OperationFailedException("查询借阅记录失败，请稍后再试。", e);
        }

        if (loan == null) {
            logger.warn("未找到借阅记录，ID: {}", loanId);
            throw new ResourceNotFoundException("未找到ID为 " + loanId + " 的借阅记录。");
        }
        return convertToLoanDTO(loan);
    }

    @Override
    public PageDTO<LoanDTO> getLoansByUserId(int userId, int page, int pageSize)
            throws ResourceNotFoundException, ValidationException, OperationFailedException {

        if (page <= 0) {
            throw new ValidationException("页码必须是正整数。", Collections.singletonList(new ApiErrorResponse.FieldErrorDetail("page", "页码必须大于0")));
        }
        if (pageSize <= 0) {
            throw new ValidationException("每页大小必须是正整数。", Collections.singletonList(new ApiErrorResponse.FieldErrorDetail("pageSize", "每页大小必须大于0")));
        }

        // 检查用户是否存在
        try {
            if (userDAO.findUserById(userId) == null) {
                logger.warn("查询用户借阅记录失败：未找到用户ID {}", userId);
                throw new ResourceNotFoundException("未找到ID为 " + userId + " 的用户。");
            }
        } catch (Exception e) {
            logger.error("查询用户借阅记录时查找用户ID {} 失败。", userId, e);
            throw new OperationFailedException("查找用户信息时发生错误。", e);
        }

        logger.debug("获取用户ID {} 的借阅记录分页列表 - 页码: {}, 每页大小: {}", userId, page, pageSize);
        List<Loan> loans;
        long totalLoans;

        try {
            loans = loanDAO.findLoansByUserId(userId, page, pageSize);
            totalLoans = loanDAO.countLoansByUserId(userId);
        } catch (Exception e) {
            logger.error("Service层获取用户ID {} 的借阅列表时发生数据库错误。", userId, e);
            throw new OperationFailedException("获取用户借阅列表失败，请稍后再试。", e);
        }

        List<LoanDTO> loanDTOs = loans.stream()
                .map(this::convertToLoanDTO)
                .collect(Collectors.toList());

        return new PageDTO<>(loanDTOs, totalLoans, page, pageSize);
    }
}