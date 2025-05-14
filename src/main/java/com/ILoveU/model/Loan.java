package com.ILoveU.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.sql.Timestamp;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "loans")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "loan_id")
    private Integer loanId;

    // 推荐对关联对象使用懒加载
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 属性类型为User，名称为user


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book; // 属性类型为Book，名称为book

    /**
     * 借出日期和时间。
     * 对应数据库中的 loan_date 列 (或 checkout_date，根据实际列名调整)。
     * 对应API规范中的 checkoutDate。
     */
    @Column(name = "loan_date", nullable = false) // 假设数据库列名为 loan_date
    private Timestamp loanDate; // 字段名改为loanDate (或checkoutDate)

    /**
     * 应归还日期和时间。
     * 对应数据库中的 due_date 列。
     * 对应API规范中的 dueDate。
     */
    @Column(name = "due_date", nullable = false)
    private Timestamp dueDate;

    /**
     * 实际归还日期和时间。
     * 对应数据库中的 return_date 列。
     * 此字段允许为NULL，因为书可能尚未归还。
     * 对应API规范中的 returnDate。
     */
    @Column(name = "return_date", nullable = true) // nullable = true，因为归还前此值为NULL
    private Timestamp returnDate;


}
