package com.ILoveU.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.sql.Timestamp;


/**
 * 表示一次图书借阅记录的实体类。
 * 包含与User和Book的关联，并冗余存储借阅时的图书标题。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "loans")
public class Loan {

    /**
     * 借阅记录的唯一标识符 (主键, 自增)。
     * 对应数据库中的 loan_id 列。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "loan_id")
    private Integer loanId;

    /**
     * 借阅此书的用户。
     * 这是一个多对一的关系：一个用户可以有多条借阅记录。
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false) // user_id 通常不应为null
    private User user;

    /**
     * 被借阅的图书。
     * 这是一个多对一的关系：一本书可以有多条借阅记录。
     * 如果关联的Book被删除，此字段在数据库中会因 ON DELETE SET NULL 而变为NULL。
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = true) // 允许为null
    private Book book;

    /**
     * 借阅时记录的图书标题。
     * 这是一个冗余字段，用于在关联的Book被删除 (book_id变为NULL) 后，仍能提供书名参考。
     * 此字段在创建Loan记录时由应用程序逻辑填充。
     */
    @Column(name = "borrowed_book_title", length = 255) // 修改点2: 新增字段
    private String borrowedBookTitle;

    /**
     * 借出日期和时间。
     * 对应数据库中的 loan_date 列。
     */
    @Column(name = "loan_date", nullable = false)
    private Timestamp loanDate;

    /**
     * 应归还日期和时间。
     * 对应数据库中的 due_date 列。
     */
    @Column(name = "due_date", nullable = false)
    private Timestamp dueDate;

    /**
     * 实际归还日期和时间。
     * 此字段允许为NULL，因为书可能尚未归还。
     */
    @Column(name = "return_date", nullable = true)
    private Timestamp returnDate;
}

