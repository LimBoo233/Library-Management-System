package com.ILoveU.dao.impl;

import com.ILoveU.dao.AuthorDAO;
import com.ILoveU.model.Author;
import com.ILoveU.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class AuthorDAOImpl implements AuthorDAO {

    private static final Logger logger = LoggerFactory.getLogger(AuthorDAOImpl.class);

    @Override
    public List<Author> findAuthors(int page, int pageSize) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            // 可以根据需要添加 ORDER BY 子句，例如按名称排序: "ORDER BY a.name ASC"
            String hql = "FROM Author a ORDER BY a.lastName ASC, a.firstName ASC";
            Query<Author> query = session.createQuery(hql, Author.class);

            query.setFirstResult((page - 1) * pageSize);
            query.setMaxResults(pageSize);

            return query.list();
        } catch (Exception e) {
            logger.error("查询作者时发生错误: {}", e.getMessage(), e);
        }

        return Collections.emptyList();
    }

    @Override
    public Author findAuthorById(int authorId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(Author.class, authorId);
        } catch (Exception e) {
            logger.error("通过ID {} 查询作者时发生错误: {}", authorId, e.getMessage(), e);
        }
        return null;
    }

    @Override
    public Author addAuthor(Author author) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.save(author);
            transaction.commit();
            return author;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            logger.error("添加作者时发生错误: {}", e.getMessage(), e);
        }
        return null;
    }

    @Override
    public Author updateAuthor(Author author) {
        Transaction transaction = null;
        Author updatedAuthor;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            updatedAuthor = (Author) session.merge(author);
            transaction.commit();
            return updatedAuthor;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            logger.error("更新作者 ID: {} 时发生错误: {}", author.getAuthorId(), e.getMessage(), e);
        }

        return null;
    }

    @Override
    public boolean deleteAuthor(int authorId) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            Author author = session.get(Author.class, authorId);
            if (author != null) {
                session.delete(author);
                transaction.commit();
                logger.info("作者 ID: {}, 名称: {} {} 已成功从数据库删除", author.getAuthorId(), author.getFirstName(), author.getLastName());
                return true;
            } else {
                logger.warn("尝试删除作者失败：未找到ID为 {} 的作者", authorId);
                // 如果没找到，也需要提交（或回滚）事务，虽然没做任何修改
                if (transaction.isActive()) transaction.commit();
                return false;
            }
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            logger.error("删除作者 ID: {} 时发生错误: {}", authorId, e.getMessage(), e);
        }

        return false;
    }

    @Override
    public long countTotalAuthors() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT COUNT(a) FROM Author a";
            Query<Long> query = session.createQuery(hql, Long.class);
            return query.uniqueResultOptional().orElse(0L);
        } catch (Exception e) {
            logger.error("统计作者总数时发生错误: {}", e.getMessage(), e);
        }
        return 0L;
    }

    @Override
    public boolean existsByNameIgnoreCase(String firstName, String lastName) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT COUNT(a) FROM Author a WHERE lower(a.firstName) = lower(:firstNameParam) and lower(a.lastName) = lower(:lastNameParam)";
            Query<Long> query = session.createQuery(hql, Long.class);
            query.setParameter("firstNameParam", firstName);
            query.setParameter("lastNameParam", lastName);

            Long count = query.uniqueResult();
            return count != null && count > 0;
        } catch (Exception e) {
            logger.error("检查作者是否存在时发生错误: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public List<Author> findAuthorsByNameKeyword(String nameKeyword, int page, int pageSize) {
        // 如果关键词为空或仅包含空白，则退化为查询所有作者
        if (nameKeyword == null || nameKeyword.trim().isEmpty()) {
            logger.debug("关键词为空，调用 findAuthors 进行分页查询。");
            return findAuthors(page, pageSize);
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // 准备模糊匹配的关键词模式，通常在关键词前后加上 '%'
            // 并转换为小写，以配合HQL中的 lower() 函数实现不区分大小写的搜索
            String keywordPattern = "%" + nameKeyword.toLowerCase().trim() + "%";

            String hql = "FROM Author a WHERE lower(a.firstName) LIKE :keyword OR lower(a.lastName) LIKE :keyword ORDER BY a.lastName ASC, a.firstName ASC";

            Query<Author> query = session.createQuery(hql, Author.class);
            query.setParameter("keyword", keywordPattern);

            // 设置分页参数
            query.setFirstResult((page - 1) * pageSize);
            query.setMaxResults(pageSize);

            List<Author> authors = query.list();
            logger.info("按关键词 '{}' 分页查询到 {} 条作者记录。页码: {}, 每页大小: {}", nameKeyword, authors.size(), page, pageSize);
            return authors;

        } catch (Exception e) {
            logger.error("按关键词 '{}' 分页查询作者时发生错误: page={}, pageSize={}. 错误: {}", nameKeyword, page, pageSize, e.getMessage(), e);
        }

        return Collections.emptyList();
    }

    @Override
    public long countAuthorsByNameKeyword(String nameKeyword) {
        // 如果关键词为空或仅包含空白，返回0L
        if (nameKeyword == null || nameKeyword.trim().isEmpty()) {
            logger.debug("关键词为空，返回0。");
            return 0L;
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // 并转换为小写，以配合HQL中的 lower() 函数实现不区分大小写的搜索
            String keywordPattern = "%" + nameKeyword.toLowerCase().trim() + "%";
            String hql = "SELECT COUNT(a) FROM Author a WHERE lower(a.firstName) LIKE :keyword OR lower(a.lastName) LIKE :keyword";

            Query<Long> query = session.createQuery(hql, Long.class);
            query.setParameter("keyword", keywordPattern);

            return query.uniqueResultOptional().orElse(0L);
        } catch (Exception e) {
            logger.error("按关键词 '{}' 统计作者总数时发生错误: {}", nameKeyword, e.getMessage(), e);
        }

        return 0L;
    }

    @Override
    public List<Author> findAuthorsByIds(Set<Integer> authorIds) {
        if (authorIds == null || authorIds.isEmpty()) {
            logger.debug("findAuthorsByIds: 提供的作者ID集合为空或null。");
            return Collections.emptyList();
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM Author a WHERE a.authorId IN (:ids)";
            Query<Author> query = session.createQuery(hql, Author.class);
            // 使用 setParameterList 来设置集合类型的参数
            query.setParameterList("ids", authorIds);

            List<Author> authors = query.list();
            logger.debug("通过ID列表 {} 查询到 {} 位作者。", authorIds, authors.size());
            return authors;
        } catch (Exception e) {
            logger.error("根据ID列表查询作者时发生错误: {}", e.getMessage(), e);
        }
        return Collections.emptyList();
    }
}
