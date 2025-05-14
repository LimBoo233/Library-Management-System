package com.ILoveU.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.sql.Timestamp;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "books")
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "book_id")
    private Integer bookId;

    @Column(name = "publish_year", nullable = false, length = 4)
    private Integer publicationYear;

    @Column(name = "num_copies_total", nullable = false, length = 100)
    private Integer numCopiesTotal;

    @Column(name = "num_copies_available", nullable = false, length = 100)
    private Integer numCopiesAvailable;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    @Column(name = "updated_at", nullable = false)
    private Timestamp updateAt;

    @ManyToOne
    @JoinColumn(name = "press_id", nullable = false)
    private Press press;
}
