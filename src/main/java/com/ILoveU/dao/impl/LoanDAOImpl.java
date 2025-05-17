package com.ILoveU.dao.impl;

import com.ILoveU.dao.LoanDAO;
import com.ILoveU.model.Loan;
import com.ILoveU.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

public class LoanDAOImpl implements LoanDAO {

    private static final Logger logger = LoggerFactory.getLogger(LoanDAOImpl.class);

    @Override
    public Loan addLoan(Loan loan) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.save(loan);
            transaction.commit();
            // 日志记录，包含loanId, userId, bookId
            logger.info("新的借阅记录 (ID: {}) 已成功添加，用户ID: {}, 图书ID: {}",
                    loan.getLoanId(),
                    loan.getUser() != null ? loan.getUser().getId() : "N/A",
                    loan.getBook() != null ? loan.getBook().getBookId() : "N/A");
            return loan;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            logger.error("添加借阅记录时发生错误: {}", e.getMessage(), e);
        }
        return null;
    }

    @Override
    public Loan findLoanById(int loanId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(Loan.class, loanId);
        } catch (Exception e) {
            logger.error("通过ID {} 查询借阅记录时发生错误: {}", loanId, e.getMessage(), e);
        }
        return null;
    }

    @Override
    public Loan updateLoan(Loan loan) {
        Transaction transaction = null;
        Loan updatedLoan;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            updatedLoan = (Loan) session.merge(loan);
            transaction.commit();
            logger.info("借阅记录 ID: {} 已成功更新。", updatedLoan.getLoanId());
            return updatedLoan;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            logger.error("更新借阅记录 ID: {} 时发生错误: {}", loan.getLoanId(), e.getMessage(), e);
        }

        return null;
    }

    @Override
    public long countActiveLoansByBookId(int bookId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // HQL查询: 统计指定bookId且returnDate为NULL的借阅记录数量
            // "l.book.bookId" 是因为Loan实体中关联的是Book对象，其ID属性是bookId
            String hql = "SELECT COUNT(l.id) FROM Loan l WHERE l.book.bookId = :bookIdParam AND l.returnDate IS NULL";
            Query<Long> query = session.createQuery(hql, Long.class);
            query.setParameter("bookIdParam", bookId);
            long count = query.uniqueResultOptional().orElse(0L);
            logger.debug("图书ID {} 的活动借阅记录数量为: {}", bookId, count);
            return count;
        } catch (Exception e) {
            logger.error("统计图书ID {} 的活动借阅记录时发生错误: {}", bookId, e.getMessage(), e);
        }
        return 0L;
    }

    @Override
    public List<Loan> findLoansByUserId(int userId, int page, int pageSize) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // HQL查询: 按userId分页查询，并按借阅日期降序排列
            String hql = "FROM Loan l WHERE l.user.id = :userIdParam ORDER BY l.loanDate DESC";
            Query<Loan> query = session.createQuery(hql, Loan.class);
            query.setParameter("userIdParam", userId);
            query.setFirstResult((page - 1) * pageSize);
            query.setMaxResults(pageSize);
            List<Loan> loans = query.list();
            logger.debug("为用户ID {} 查询到 {} 条借阅记录 (页码: {}, 每页大小: {})", userId, loans.size(), page, pageSize);
            return loans;
        } catch (Exception e) {
            logger.error("为用户ID {} 分页查询借阅记录时发生错误: {}", userId, e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    @Override
    public long countLoansByUserId(int userId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT COUNT(l.id) FROM Loan l WHERE l.user.id = :userIdParam";
            Query<Long> query = session.createQuery(hql, Long.class);
            query.setParameter("userIdParam", userId);
            long count = query.uniqueResultOptional().orElse(0L);
            logger.debug("用户ID {} 的借阅记录总数为: {}", userId, count);
            return count;
        } catch (Exception e) {
            logger.error("统计用户ID {} 的借阅记录总数时发生错误: {}", userId, e.getMessage(), e);
        }
        return 0L;
    }

    @Override
    public List<Loan> findAllLoans(int page, int pageSize) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM Loan l ORDER BY l.loanDate DESC"; // 按借阅日期降序排列
            Query<Loan> query = session.createQuery(hql, Loan.class);
            query.setFirstResult((page - 1) * pageSize);
            query.setMaxResults(pageSize);
            List<Loan> loans = query.list();
            logger.debug("查询到 {} 条所有借阅记录 (页码: {}, 每页大小: {})", loans.size(), page, pageSize);
            return loans;
        } catch (Exception e) {
            logger.error("获取所有借阅记录时发生错误: {}", e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    @Override
    public long countAllLoans() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT COUNT(l.id) FROM Loan l";
            Query<Long> query = session.createQuery(hql, Long.class);
            return query.uniqueResultOptional().orElse(0L);
        } catch (Exception e) {
            logger.error("统计借阅记录总数时发生错误: {}", e.getMessage(), e);
        }
        return 0L;
    }
}
