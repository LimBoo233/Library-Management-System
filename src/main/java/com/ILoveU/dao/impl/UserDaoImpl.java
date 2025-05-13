package com.ILoveU.dao.impl;

import com.ILoveU.dao.UserDAO;
import com.ILoveU.model.User;
import com.ILoveU.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import com.ILoveU.util.Log;

public class UserDaoImpl implements UserDAO {
    @Override
    public User addUser(User user) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // 获取 Session
            // 开启事务
            transaction = session.beginTransaction();
            // 保存用户
            session.save(user);
            // 提交事务
            transaction.commit();
            return user;
        } catch (Exception e) {
            // 如果发生异常，回滚事务
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
            return null;
        }
        // 关闭 Session
    }

    @Override
    public User findUserById(int id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM User U WHERE U.id = :idParam";

            Query<User> query = session.createQuery(hql, User.class);
            query.setParameter("idParam", id);

            return query.uniqueResultOptional().orElse(null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public User findUserByAccount(String account) {
        // 使用 try-with-resources 语句确保Session被正确关闭
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // 使用HQL (Hibernate Query Language) 进行查询
            // "FROM User" 中的 "User" 是实体类的名称，而不是数据库表的名称
            // "U.account" 中的 "account" 是User实体类中的属性名
            String hql = "FROM User U WHERE U.account = :accountParam";

            // 创建查询对象，并指定返回类型为User.class
            Query<User> query = session.createQuery(hql, User.class);
            // 设置查询参数 :accountParam 的值
            query.setParameter("accountParam", account);

            // uniqueResultOptional() 返回一个 Optional<User>
            // 如果没有找到结果，Optional为空；如果找到一个，Optional包含该结果
            // 如果找到多个结果，会抛出 NonUniqueResultException (如果账户名不是唯一的，这里需要注意)
            // 假设账户名在数据库中是唯一的

            return query.uniqueResultOptional().orElse(null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean isAccountExists(String account) {
        // 使用 try-with-resources 语句确保Session被正确关闭
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // 使用HQL COUNT查询来检查账户是否存在，这通常比获取整个实体更高效
            // "SELECT COUNT(U.id)" 查询符合条件的User实体的id数量
            // "FROM User U" User是实体类名, U是别名
            // "WHERE U.account = :accountParam" account是User实体中的属性名
            String hql = "SELECT COUNT(U.id) FROM User U WHERE U.account = :accountParam";

            Query<Long> query = session.createQuery(hql, Long.class); // COUNT查询返回Long类型
            query.setParameter("accountParam", account);

            // uniqueResult() 会返回单个结果 (在这里是计数)
            Long count = query.uniqueResult();

            return count != null && count > 0;
        } catch (Exception e) {
            e.printStackTrace();
            // 在发生错误时，保守地返回false或根据业务需求抛出异常
            return false;
        }
    }
    
}
