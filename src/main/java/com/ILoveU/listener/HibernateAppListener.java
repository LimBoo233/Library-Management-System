package com.ILoveU.listener;

import com.ILoveU.util.HibernateUtil;
import jakarta.servlet.ServletContextListener;

public class HibernateAppListener implements ServletContextListener {

    public void contextDestroyed(ServletContextListener servletContextListener) {
        HibernateUtil.shutdown();
    }

}
