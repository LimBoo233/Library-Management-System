package com.ILoveU.dao.impl;

import com.ILoveU.dao.BookDAO;
import com.ILoveU.exception.OperationFailedException;
import com.ILoveU.model.Book;

import java.util.Collections;
import java.util.List;

public class BookDAOImpl implements BookDAO {

    // todo

    @Override
    public Book addBook(Book book) throws OperationFailedException {
        return null;
    }

    @Override
    public Book findBookById(int bookId) throws OperationFailedException {
        return null;
    }

    @Override
    public Book updateBook(Book book) throws OperationFailedException {
        return null;
    }

    @Override
    public boolean deleteBook(int bookId) throws OperationFailedException {
        return false;
    }

    @Override
    public List<Book> findBooks(String searchKeyword, Integer pressId, Integer tagId, int page, int pageSize) throws OperationFailedException {
        return Collections.emptyList();
    }

    @Override
    public long countBooks(String searchKeyword, Integer pressId, Integer tagId) throws OperationFailedException {
        return 0;
    }

    @Override
    public long countBooksByPressId(int pressId) throws OperationFailedException {
        return 0;
    }

    @Override
    public long countBooksByAuthorId(int authorId) throws OperationFailedException {
        return 0;
    }

    @Override
    public long countBooksByTagId(int tagId) throws OperationFailedException {
        return 0;
    }

    @Override
    public Book findBookByIsbn(String isbn) throws OperationFailedException {
        return null;
    }

    @Override
    public boolean existsByIsbn(String isbn) throws OperationFailedException {
        return false;
    }

    @Override
    public boolean existsByIsbnAndNotBookId(String isbn, int excludeBookId) throws OperationFailedException {
        return false;
    }
}
