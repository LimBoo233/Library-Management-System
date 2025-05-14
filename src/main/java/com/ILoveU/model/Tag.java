package com.ILoveU.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tags")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tag_id")
    private Integer tagId;

    @Column(name = "name", nullable = false, length = 100, unique = true)
    private String name;

    @ManyToMany(mappedBy = "tags", fetch = FetchType.LAZY)
    // "tags" 是 Book 类中 @ManyToMany 注解标记的那个 Set<Tag> 属性的名称
    private Set<Book> books = new HashSet<>();


    // 为了让Book中的辅助方法能访问books集合，提供一个包级私有或受保护的getter
    Set<Book> getBooksInternal() { // 包级私有
        return books;
    }

    // 辅助方法来管理与Book的双向关联
    public void addBook(Book book) {
        if (book != null) {
            this.books.add(book);
            book.getTagsInternal().add(this); // 调用Book中用于同步的getter
        }
    }

    public void removeBook(Book book) {
        if (book != null) {
            this.books.remove(book);
            book.getTagsInternal().remove(this);
        }
    }

    // 重写 equals 和 hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tag tag = (Tag) o;
        return Objects.equals(tagId, tag.tagId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tagId);
    }

}
