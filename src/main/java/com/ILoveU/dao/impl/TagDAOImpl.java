package com.ILoveU.dao.impl;

import com.ILoveU.dao.TagDAO;

import com.ILoveU.model.Tag;
import com.ILoveU.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Set;


public class TagDAOImpl implements TagDAO {

    private static final Logger logger = LoggerFactory.getLogger(TagDAOImpl.class);

    @Override
    public Tag addTag(Tag tag) {
        Transaction transaction = null;
        if (tag == null) {
            logger.warn("尝试添加的Tag对象为null。");
            return null;
        }
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.save(tag);
            transaction.commit();
            logger.info("标签 '{}' (ID: {}) 已成功添加到数据库。", tag.getName(), tag.getTagId());
            return tag;
        } catch (Exception e) { // 包括 ConstraintViolationException 等
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            logger.error("添加标签 '{}' 时发生错误: {}", tag.getName(), e.getMessage(), e);
        }
        return null;
    }

    @Override
    public Tag findTagById(int tagId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Tag tag = session.get(Tag.class, tagId);
            if (tag != null) {
                logger.debug("通过ID {} 找到标签: {}", tagId, tag.getName());
            } else {
                logger.debug("未找到ID为 {} 的标签。", tagId);
            }
            return tag;
        } catch (Exception e) {
            logger.error("通过ID {} 查询标签时发生错误: {}", tagId, e.getMessage(), e);
        }
        return null;
    }

    @Override
    public List<Tag> findTagsByIds(Set<Integer> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return Collections.emptyList();
        }
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // 使用 "IN (:ids)" 子句来查询多个ID
            String hql = "FROM Tag t WHERE t.tagId IN (:ids)";
            Query<Tag> query = session.createQuery(hql, Tag.class);
            query.setParameterList("ids", tagIds); // 使用 setParameterList 处理集合参数
            return query.list();
        } catch (Exception e) {
            logger.error("根据ID列表查询标签时发生错误: {}", e.getMessage(), e);
        }
        return Collections.emptyList();
    }


    @Override
    public Tag updateTag(Tag tag) {
        Transaction transaction = null;
        if (tag == null || tag.getTagId() == null) {
            logger.warn("尝试更新的Tag对象或其ID为null。");
            return null;
        }
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            Tag managedTag = (Tag) session.merge(tag);
            transaction.commit();
            logger.info("标签 ID: {} 已成功更新，新名称为: '{}'。", managedTag.getTagId(), managedTag.getName());
            return managedTag;
        } catch (Exception e) { // 包括 ConstraintViolationException 等
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            logger.error("更新标签 ID: {} 时发生错误: {}", tag.getTagId(), e.getMessage(), e);
        }
        return null;
    }

    @Override
    public boolean deleteTag(int tagId) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            Tag tag = session.get(Tag.class, tagId);
            if (tag != null) {
                session.delete(tag);
                transaction.commit();
                logger.info("标签 ID: {} 已成功从数据库删除。", tagId);
                return true;
            } else {
                logger.warn("尝试删除标签失败：未找到ID为 {} 的标签。", tagId);
                if (transaction != null && transaction.isActive()) {
                    transaction.commit(); // 事务中无修改，提交是安全的
                }
                return false;
            }
        } catch (Exception e) { // 例如，如果因为外键约束（如果Tag被Book引用且没有配置级联删除或SET NULL）
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            logger.error("删除标签 ID: {} 时发生错误: {}", tagId, e.getMessage(), e);
        }
        return false;
    }

    @Override
    public List<Tag> findTags(int page, int pageSize) {
        // 假设参数 page 和 pageSize 的基本校验（例如 > 0）已在Service层完成。
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM Tag t ORDER BY t.name ASC"; // 按名称排序
            Query<Tag> query = session.createQuery(hql, Tag.class);
            query.setFirstResult((page - 1) * pageSize);
            query.setMaxResults(pageSize);
            return query.list();
        } catch (Exception e) {
            logger.error("分页查询标签列表时发生错误: page={}, pageSize={}", page, pageSize, e);
        }
        return Collections.emptyList();
    }

    @Override
    public List<Tag> findAllTags() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM Tag t ORDER BY t.name ASC";
            Query<Tag> query = session.createQuery(hql, Tag.class);
            return query.list();
        } catch (Exception e) {
            logger.error("获取所有标签时发生错误: {}", e.getMessage(), e);
        }
        return Collections.emptyList();
    }


    @Override
    public long countTotalTags() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT COUNT(t.id) FROM Tag t";
            Query<Long> query = session.createQuery(hql, Long.class);
            return query.uniqueResultOptional().orElse(0L);
        } catch (Exception e) {
            logger.error("统计标签总数时发生错误: {}", e.getMessage(), e);
        }
        return 0L;
    }

    @Override
    public boolean existsByNameIgnoreCase(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT COUNT(t.id) FROM Tag t WHERE lower(t.name) = lower(:nameParam)";
            Query<Long> query = session.createQuery(hql, Long.class);
            query.setParameter("nameParam", name.trim());
            return query.uniqueResultOptional().orElse(0L) > 0;
        } catch (Exception e) {
            logger.error("检查标签名称 '{}' 是否存在时发生错误: {}", name, e.getMessage(), e);
        }
        return false; // 出错时保守返回false
    }

    @Override
    public Tag findTagByNameIgnoreCase(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM Tag t WHERE lower(t.name) = lower(:nameParam)";
            Query<Tag> query = session.createQuery(hql, Tag.class);
            query.setParameter("nameParam", name.trim());
            // 标签名应该是唯一的，所以uniqueResultOptional是合适的
            return query.uniqueResultOptional().orElse(null);
        } catch (Exception e) {
            logger.error("通过名称 '{}' 查找标签时发生错误: {}", name, e.getMessage(), e);
        }
        return null;
    }
}
