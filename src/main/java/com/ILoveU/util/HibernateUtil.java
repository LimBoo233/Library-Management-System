// 放在 com.example.util 包下
package com.ILoveU.util;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {
    private static final SessionFactory sessionFactory;

    static {
        try {
            // 创建SessionFactory，默认会读取 hibernate.cfg.xml 文件
            sessionFactory = new Configuration().
                    configure().
                    buildSessionFactory();
        } catch (Throwable ex) {
            // 记录初始化失败的日志
            Log.Instance().severe("Initial SessionFactory creation failed." + ex);
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
        }
    }
}