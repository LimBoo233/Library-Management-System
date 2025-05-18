package com.ILoveU.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 表示图书的实体类。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "books", uniqueConstraints = {
        @UniqueConstraint(columnNames = "isbn", name = "uk_book_isbn") // 为ISBN添加唯一约束
})
public class Book {

    /**
     * 图书的唯一标识符 (主键, 自增)。
     * 对应数据库中的 book_id 列。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "book_id")
    private Integer bookId;

    /**
     * 图书的标题。
     * 对应数据库中的 title 列，不能为空，最大长度255。
     */
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    /**
     * 图书的国际标准书号 (ISBN)。
     * 对应数据库中的 isbn 列，不能为空，长度13，且唯一。
     */
    @Column(name = "isbn", nullable = false, length = 13) // unique = true 已在@Table中定义
    private String isbn;

    /**
     * 图书的出版年份。
     * 对应数据库中的 publication_year 列 (YEAR类型)。
     */
    @Column(name = "publish_year") // 移除了length属性，YEAR类型由数据库定义
    private Integer publishYear; // ERD图是 publication_year

    /**
     * 图书的总副本数量。
     * 对应数据库中的 num_copies_total 列，不能为空。
     */
    @Column(name = "num_copies_total", nullable = false) // 移除了length属性
    private Integer numCopiesTotal;

    /**
     * 图书当前可供借阅的副本数量。
     * 对应数据库中的 num_copies_available 列，不能为空。
     */
    @Column(name = "num_copies_available", nullable = false) // 移除了length属性
    private Integer numCopiesAvailable;

    /**
     * 记录创建时间。由Hibernate自动填充。
     * 对应数据库中的 created_at 列。
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false) // 创建时间不应被更新
    private Timestamp createdAt; // 或者 LocalDateTime

    /**
     * 记录最后更新时间。由Hibernate自动填充。
     * 对应数据库中的 updated_at 列。
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Timestamp updatedAt; // 修正了字段名，或者 LocalDateTime

    /**
     * 图书所属的出版社。
     * 这是一个多对一的关系：多本书可以属于一个出版社。
     */

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "press_id", nullable = false) // 外键列在books表中
    private Press press;

    /**
     * 图书的作者集合。
     * 这是一个多对多关系，通过中间表 book_authors 连接。
     */
    @ManyToMany(fetch =  FetchType.EAGER, cascade = { CascadeType.MERGE }) // 添加级联
    @JoinTable(
            name = "book_authors", // 中间连接表的名称
            joinColumns = @JoinColumn(name = "book_id"), // 中间表中参照当前实体(Book)主键的外键列
            inverseJoinColumns = @JoinColumn(name = "author_id") // 中间表中参照目标实体(Author)主键的外键列
    )
    private Set<Author> authors = new HashSet<>();

    /**
     * 图书的标签集合。
     * 这是一个多对多关系，通过中间表 book_tags 连接。
     */
    @ManyToMany(fetch =  FetchType.EAGER, cascade = { CascadeType.MERGE })
    @JoinTable(
            name = "book_tags",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();


    // 辅助方法来管理与Author的双向关联
    public void addAuthor(Author author) {
        if (author != null) {
            this.authors.add(author);
            author.getBooksInternal().add(this); // 假设Author有getBooksInternal()
        }
    }

    public void removeAuthor(Author author) {
        if (author != null) {
            this.authors.remove(author);
            author.getBooksInternal().remove(this);
        }
    }

    // 辅助方法来管理与Tag的双向关联
    public void addTag(Tag tag) {
        if (tag != null) {
            this.tags.add(tag);
            tag.getBooksInternal().add(this); // 假设Tag有getBooksInternal()
        }
    }

    public void removeTag(Tag tag) {
        if (tag != null) {
            this.tags.remove(tag);
            tag.getBooksInternal().remove(this);
        }
    }

    // 为了让Author和Tag中的辅助方法能访问books集合，提供一个包级私有或受保护的getter
    // 或者直接使用公有getter，但在Author/Tag的辅助方法中要小心处理
    Set<Tag> getTagsInternal() { // 包级私有，仅用于Author/Tag的辅助方法同步
        return tags;
    }
    Set<Author> getAuthorsInternal() { // 包级私有
        return authors;
    }


    // 重写 equals 和 hashCode 对于在Set中正确操作实体非常重要
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Book book = (Book) o;
        return Objects.equals(bookId, book.bookId); // 通常基于ID判断相等性
    }

    @Override
    public int hashCode() {
        return Objects.hash(bookId); // 通常基于ID生成哈希码
    }


}
