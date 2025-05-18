package com.ILoveU.dao.impl;

import com.ILoveU.dao.BookDAO;
import com.ILoveU.exception.OperationFailedException;
import com.ILoveU.model.Book;
import com.ILoveU.model.Loan;
import com.ILoveU.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class BookDAOImpl implements BookDAO {
    private static final Logger logger = LoggerFactory.getLogger(BookDAOImpl.class);

    @Override
    public Book addBook(Book book) {
        Transaction transaction = null;
        if (book == null) {
            logger.warn("尝试添加的Book对象为null。");
            return null;
        }
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            // Persists the Book entity and cascades to authors/tags if configured
            session.save(book);
            transaction.commit();
            logger.info("图书 '{}' (ID: {}) 已成功添加到数据库。", book.getTitle(), book.getBookId());
            return book;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            logger.error("添加图书 '{}' 时发生错误: {}", book.getTitle(), e.getMessage(), e);
        }
        return null;
    }

    @Override
    public Book findBookById(int bookId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Book book = session.get(Book.class, bookId);
            if (book != null) {
                logger.debug("通过ID {} 找到图书: {}", bookId, book.getTitle());
            } else {
                logger.debug("未找到ID为 {} 的图书。", bookId);
            }
            return book;
        } catch (Exception e) {
            logger.error("通过ID {} 查询图书时发生错误: {}", bookId, e.getMessage(), e);
        }
        return null;
    }

    @Override
    public Book updateBook(Book book) {
        Transaction transaction = null;
        Book managedBook = null;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            managedBook = (Book) session.merge(book);
            transaction.commit();
            logger.info("图书 ID: {} 已成功更新。", managedBook.getBookId());
            return managedBook;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            logger.error("更新图书 ID: {} 时发生错误: {}", book.getBookId(), e.getMessage(), e);
        }
        return null;
    }

    @Override
    public boolean deleteBook(int bookId) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            Book book = session.get(Book.class, bookId);
            if (book != null) {
                // Hibernate will handle deletion from join tables for @ManyToMany if Book is owning side or cascade is set appropriately
                session.delete(book);
                transaction.commit();
                logger.info("图书 ID: {} 已成功从数据库删除。", bookId);
                return true;
            } else {
                logger.warn("尝试删除图书失败：未找到ID为 {} 的图书。", bookId);
                if (transaction != null && transaction.isActive()) {
                    transaction.commit(); // No changes made, commit is safe
                }
                return false;
            }
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            logger.error("删除图书 ID: {} 时发生错误: {}", bookId, e.getMessage(), e);
        }
        return false;
    }

    @Override
    public List<Book> findBooks(String searchKeyword, Integer pressId, Integer tagId, int page, int pageSize) {

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            StringBuilder hqlBuilder = new StringBuilder("SELECT DISTINCT b FROM Book b ");
            Map<String, Object> parameters = new HashMap<>();
            List<String> whereClauses = new ArrayList<>();

            // Join for filtering/searching by author or tag if necessary
            boolean authorJoined = false;
            boolean tagJoined = false;

            if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
                // For searching by author name, a join is needed
                hqlBuilder.append("LEFT JOIN b.authors auth ");
                authorJoined = true;

            }
            if (tagId != null) {
                hqlBuilder.append("JOIN b.tags t_filter "); // Alias for tag filtering
                tagJoined = true; // Mark that tags table is joined for filtering
            }


            // Build WHERE clauses
            if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
                String keywordPattern = "%" + searchKeyword.toLowerCase().trim() + "%";
                String searchCondition = "(lower(b.title) LIKE :keyword";
                if (authorJoined) {
                    searchCondition += " OR lower(auth.firstName) LIKE :keyword OR lower(auth.lastName) LIKE :keyword";
                }
                searchCondition += ")";
                whereClauses.add(searchCondition);
                parameters.put("keyword", keywordPattern);
            }

            if (pressId != null) {
                whereClauses.add("b.press.id = :pressId");
                parameters.put("pressId", pressId);
            }

            if (tagId != null) {
                whereClauses.add("t_filter.id = :tagId"); // Use the alias from the JOIN for tagId filter
                parameters.put("tagId", tagId);
            }

            if (!whereClauses.isEmpty()) {
                hqlBuilder.append(" WHERE ").append(String.join(" AND ", whereClauses));
            }

            hqlBuilder.append(" ORDER BY b.title ASC"); // Default ordering

            Query<Book> query = session.createQuery(hqlBuilder.toString(), Book.class);
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                query.setParameter(entry.getKey(), entry.getValue());
            }

            query.setFirstResult((page - 1) * pageSize);
            query.setMaxResults(pageSize);

            List<Book> books = query.list();
            logger.info("动态查询图书: {}条记录，关键词='{}', 出版社ID={}, 标签ID={}, 页码={}, 每页大小={}",
                    books.size(), searchKeyword, pressId, tagId, page, pageSize);
            return books;

        } catch (Exception e) {
            logger.error("动态查询图书时发生错误: 关键词='{}', 出版社ID={}, 标签ID={}, 页码={}, 每页大小={}",
                    searchKeyword, pressId, tagId, page, pageSize, e);
        }
        return Collections.emptyList();
    }

    @Override
    public long countBooks(String searchKeyword, Integer pressId, Integer tagId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            StringBuilder hqlBuilder = new StringBuilder("SELECT COUNT(DISTINCT b.id) FROM Book b ");
            Map<String, Object> parameters = new HashMap<>();
            List<String> whereClauses = new ArrayList<>();

            boolean authorJoined = false;
            boolean tagJoined = false;

            if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
                hqlBuilder.append("LEFT JOIN b.authors auth ");
                authorJoined = true;
            }
            if (tagId != null) {
                hqlBuilder.append("JOIN b.tags t_filter ");
                tagJoined = true;
            }

            if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
                String keywordPattern = "%" + searchKeyword.toLowerCase().trim() + "%";
                String searchCondition = "(lower(b.title) LIKE :keyword";
                if (authorJoined) {
                    searchCondition += " OR lower(auth.firstName) LIKE :keyword OR lower(auth.lastName) LIKE :keyword";
                }
                searchCondition += ")";
                whereClauses.add(searchCondition);
                parameters.put("keyword", keywordPattern);
            }
            if (pressId != null) {
                whereClauses.add("b.press.id = :pressId");
                parameters.put("pressId", pressId);
            }
            if (tagId != null) {
                whereClauses.add("t_filter.id = :tagId");
                parameters.put("tagId", tagId);
            }

            if (!whereClauses.isEmpty()) {
                hqlBuilder.append(" WHERE ").append(String.join(" AND ", whereClauses));
            }

            Query<Long> query = session.createQuery(hqlBuilder.toString(), Long.class);
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                query.setParameter(entry.getKey(), entry.getValue());
            }

            long count = query.uniqueResultOptional().orElse(0L);
            logger.info("动态统计图书: {}条记录，关键词='{}', 出版社ID={}, 标签ID={}",
                    count, searchKeyword, pressId, tagId);
            return count;

        } catch (Exception e) {
            logger.error("动态统计图书时发生错误: 关键词='{}', 出版社ID={}, 标签ID={}",
                    searchKeyword, pressId, tagId, e);
        }
        return 0L;
    }

    @Override
    public long countBooksByPressId(int pressId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT COUNT(b.bookId) FROM Book b WHERE b.press.id = :pressIdParam";
            Query<Long> query = session.createQuery(hql, Long.class);
            query.setParameter("pressIdParam", pressId);
            long count = query.uniqueResultOptional().orElse(0L);
            logger.debug("出版社ID {} 的图书数量为: {}", pressId, count);
            return count;
        } catch (Exception e) {
            logger.error("统计出版社ID {} 的图书数量时发生错误: {}", pressId, e.getMessage(), e);
        }
        return 0L;
    }

    @Override
    public long countBooksByAuthorId(int authorId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // To count books by a specific author, we need to query through the join table
            // or the 'authors' collection in Book entity.
            // HQL: "SELECT COUNT(DISTINCT b.id) FROM Book b JOIN b.authors a WHERE a.id = :authorIdParam"
            String hql = "SELECT COUNT(DISTINCT b.bookId) FROM Book b JOIN b.authors author_alias WHERE author_alias.authorId = :authorIdParam";
            Query<Long> query = session.createQuery(hql, Long.class);
            query.setParameter("authorIdParam", authorId);
            long count = query.uniqueResultOptional().orElse(0L);
            logger.debug("作者ID {} 的图书数量为: {}", authorId, count);
            return count;
        } catch (Exception e) {
            logger.error("统计作者ID {} 的图书数量时发生错误: {}", authorId, e.getMessage(), e);
        }
        return 0L;
    }

    @Override
    public long countBooksByTagId(int tagId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT COUNT(DISTINCT b.bookId) FROM Book b JOIN b.tags tag_alias WHERE tag_alias.tagId = :tagIdParam";
            Query<Long> query = session.createQuery(hql, Long.class);
            query.setParameter("tagIdParam", tagId);
            long count = query.uniqueResultOptional().orElse(0L);
            logger.debug("标签ID {} 的图书数量为: {}", tagId, count);
            return count;
        } catch (Exception e) {
            logger.error("统计标签ID {} 的图书数量时发生错误: {}", tagId, e.getMessage(), e);
        }
        return 0L;
    }

    @Override
    public Book findBookByIsbn(String isbn) {
        if (isbn == null || isbn.trim().isEmpty()) {
            return null;
        }
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM Book b WHERE b.isbn = :isbnParam";
            Query<Book> query = session.createQuery(hql, Book.class);
            query.setParameter("isbnParam", isbn.trim());
            return query.uniqueResultOptional().orElse(null);
        } catch (Exception e) {
            logger.error("通过ISBN '{}' 查询图书时发生错误: {}", isbn, e.getMessage(), e);
        }
        return null;
    }

    @Override
    public boolean existsByIsbn(String isbn) {
        if (isbn == null || isbn.trim().isEmpty()) {
            return false;
        }
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT COUNT(b.bookId) FROM Book b WHERE b.isbn = :isbnParam";
            Query<Long> query = session.createQuery(hql, Long.class);
            query.setParameter("isbnParam", isbn.trim());
            return query.uniqueResultOptional().orElse(0L) > 0;
        } catch (Exception e) {
            logger.error("检查ISBN '{}' 是否存在时发生错误: {}", isbn, e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean existsByIsbnAndNotBookId(String isbn, int excludeBookId) {
        if (isbn == null || isbn.trim().isEmpty()) {
            return false;
        }
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT COUNT(b.bookId) FROM Book b WHERE b.isbn = :isbnParam AND b.bookId != :excludeBookIdParam";
            Query<Long> query = session.createQuery(hql, Long.class);
            query.setParameter("isbnParam", isbn.trim());
            query.setParameter("excludeBookIdParam", excludeBookId);
            return query.uniqueResultOptional().orElse(0L) > 0;
        } catch (Exception e) {
            logger.error("检查ISBN '{}' 是否被其他图书 (排除ID {}) 使用时发生错误: {}", isbn, excludeBookId, e.getMessage(), e);
        }
        return false;
    }
}
