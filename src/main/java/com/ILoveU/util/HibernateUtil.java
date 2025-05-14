// 放在 com.example.util 包下
package com.ILoveU.util;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HibernateUtil {
    private static final Logger logger = LoggerFactory.getLogger(HibernateUtil.class);
    private static final SessionFactory sessionFactory;

    static {
        try {
            // 创建SessionFactory，默认会读取 hibernate.cfg.xml 文件
            sessionFactory = new Configuration().
                    configure().
                    buildSessionFactory();
            logger.info("Hibernate SessionFactory初始化成功");
        } catch (Throwable ex) {
            // 记录初始化失败的日志
            logger.error("初始化SessionFactory失败: {}", ex.getMessage(), ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public static void shutdown() {
        // 关闭缓存和连接池
        if (sessionFactory != null) {
            sessionFactory.close();
            logger.info("Hibernate SessionFactory已关闭");
        }
    }
}