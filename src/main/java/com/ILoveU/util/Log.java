package com.ILoveU.util;

import java.util.logging.Logger;

public class Log {

    private static final Log instance = new Log();

    // add Handler if further features are demand
    public static Log Instance() {
        return instance;
    }

    private static final Logger logger = Logger.getLogger(Log.class.getName());

    private Log() {}

    public void info(String msg) {
        logger.info(msg);
    }

    public void severe(String msg) {
        logger.severe(msg);
    }
}
