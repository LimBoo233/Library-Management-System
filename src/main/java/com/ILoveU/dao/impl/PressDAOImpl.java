package com.ILoveU.dao.impl;

import com.ILoveU.dao.PressDAO;
import com.ILoveU.model.Press;
import com.ILoveU.util.HibernateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.ArrayList;
import java.util.List;

public class PressDAOImpl implements PressDAO {
    private static final Logger logger = LoggerFactory.getLogger(PressDAOImpl.class);

    @Override
    public List<Press> findPresses(int page, int pageSize) {
        page = Math.max(page, 1);
        pageSize = Math.max(pageSize, 1);

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            // 可以根据需要添加 ORDER BY 子句，例如按出版社名称排序: "FROM Press p ORDER BY p.name ASC"
            String hql = "FROM Press p order by p.name ASC";
            Query<Press> query = session.createQuery(hql, Press.class);

            query.setFirstResult((page - 1) * pageSize);
            query.setMaxResults(pageSize);

            return query.list();
        } catch (Exception e) {
            logger.error("查询出版社时发生错误: {}", e.getMessage(), e);
        }

        return new ArrayList<>();
    }

    @Override
    public Press findPressById(int pressId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM Press p WHERE p.pressId = :idParam";

            Query<Press> query = session.createQuery(hql, Press.class);
            query.setParameter("idParam", pressId);

            return query.uniqueResultOptional().orElse(null);
        } catch (Exception e) {
            logger.error("查询出版社时发生错误: {}", e.getMessage(), e);
        }
        return null;
    }

    @Override
    public Press addPress(Press press) {
        // TODO
        Transaction transaction = null;
        try  (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.save(press);
            transaction.commit();
            return press;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            logger.error("添加出版社时发生错误: {}", e.getMessage(), e);
        }
        return null;
    }

    @Override
    public Press updatePress(Press press) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.update(press); // 或者 session.merge(press) 如果对象是游离态
            transaction.commit();
            return press;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            logger.error("更新出版社 {} 时发生错误: {}", press.getName(), e.getMessage(), e);
        }

        return null;
    }

    @Override
    public boolean deletePress(int pressId) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            Press press = session.get(Press.class, pressId);
            if (press != null) {
                session.delete(press);
                transaction.commit();
                logger.info("出版社 ID: {}, 名称: {} 已成功从数据库删除", pressId, press.getName());
                return true;
            } else {
                logger.warn("尝试删除出版社失败：未找到ID为 {} 的出版社", pressId);
                if(transaction.isActive()) transaction.commit(); // 如果没找到，也需要提交（或回滚）事务，虽然没做任何修改
                return false;
            }
        } catch (Exception e) { // 需要捕获更具体的异常，例如 ConstraintViolationException
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            logger.error("删除出版社 ID: {} 时发生错误: {}", pressId, e.getMessage(), e);
        }
        
        return false;
    }

    @Override
    public long countTotalPresses() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT COUNT(p) FROM Press p";
            Query<Long> query = session.createQuery(hql, Long.class);
            return query.uniqueResultOptional().orElse(0L);
        } catch (Exception e) {
            logger.error("统计出版社总数时发生错误: {}", e.getMessage(), e);
        }
        return 0L;
    }

    @Override
    public boolean existsByNameIgnoreCase(String name) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT COUNT(p.name) FROM Press p WHERE p.name = :nameParam";
            Query<Long> query = session.createQuery(hql, Long.class);
            query.setParameter("nameParam", name);

            Long count = query.uniqueResult();
            return count != null && count > 0;
        } catch (Exception e) {
            logger.error("检查出版社是否存在时发生错误: {}", e.getMessage(), e);
            return false;
        }
    }

}
