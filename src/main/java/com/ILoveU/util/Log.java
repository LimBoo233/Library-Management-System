package com.ILoveU.util;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Log {

    private static final Log instance = new Log();
    private static final Logger logger = Logger.getLogger(Log.class.getName());

    static {
        // 初始化日志配置
        try {
            // 创建一个 FileHandler，将日志写入文件
            FileHandler fileHandler = new FileHandler("logs/application.log", true); // true 表示追加模式
            fileHandler.setFormatter(new SimpleFormatter()); // 设置日志格式为简单格式

            // 将 FileHandler 添加到 Logger
            logger.addHandler(fileHandler);

            // 禁用默认的控制台输出
            logger.setUseParentHandlers(false);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // add Handler if further features are demand
    public static Log Instance() {
        return instance;
    }

    private Log() {}

    public void info(String msg) {
        logger.info(msg);
    }

    public void severe(String msg) {
        logger.severe(msg);
    }
}
