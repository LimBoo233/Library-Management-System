package com.ILoveU.service;

import db.DatabaseManager;

import java.sql.*;

public class LoanService {

    /**
     * 借书事务逻辑：
     * - 检查库存
     * - 减少库存
     * - 插入借阅记录
     */
    public void checkoutBook(int userId, int bookId) throws SQLException {
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            PreparedStatement checkStock = conn.prepareStatement(
                    "SELECT num_copies_available FROM books WHERE id = ? FOR UPDATE");
            checkStock.setInt(1, bookId);
            ResultSet rs = checkStock.executeQuery();

            if (!rs.next() || rs.getInt("num_copies_available") <= 0) {
                throw new SQLException("库存不足，无法借出。");
            }

            PreparedStatement updateStock = conn.prepareStatement(
                    "UPDATE books SET num_copies_available = num_copies_available - 1 WHERE id = ?");
            updateStock.setInt(1, bookId);
            updateStock.executeUpdate();

            PreparedStatement insertLoan = conn.prepareStatement(
                    "INSERT INTO loans(user_id, book_id, checkout_date, due_date) VALUES (?, ?, NOW(), DATE_ADD(NOW(), INTERVAL 14 DAY))");
            insertLoan.setInt(1, userId);
            insertLoan.setInt(2, bookId);
            insertLoan.executeUpdate();

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) conn.close();
        }
    }

    /**
     * 还书事务逻辑：
     * - 更新归还时间
     * - 增加库存
     */
    public void returnBook(int loanId) throws SQLException {
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            PreparedStatement updateReturn = conn.prepareStatement(
                    "UPDATE loans SET return_date = NOW() WHERE id = ?");
            updateReturn.setInt(1, loanId);
            updateReturn.executeUpdate();

            PreparedStatement updateStock = conn.prepareStatement(
                    "UPDATE books SET num_copies_available = num_copies_available + 1 " +
                            "WHERE id = (SELECT book_id FROM loans WHERE id = ?)");
            updateStock.setInt(1, loanId);
            updateStock.executeUpdate();

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) conn.close();
        }
    }
}