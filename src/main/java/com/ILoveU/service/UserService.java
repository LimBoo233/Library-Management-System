package com.ILoveU.service;

import java.util.Map;

public interface UserService {

    /**
     * 用户注册服务
     * @param account 账户 (例如邮箱)
     * @param password 明文密码
     * @param name 用户姓名
     * @return 一个Map，包含操作结果:
     * - "success": (Boolean) true表示成功, false表示失败
     * - "message": (String) 操作结果的消息提示
     * - "user": (Map<String, Object>) 注册成功的用户信息 (不含密码), 如果成功
     */
    public Map<String, Object> registerUser(String name, String account, String password);

    /**
     * 用户登录服务
     * @param account 账户
     * @param password 明文密码
     * @return 一个Map，包含操作结果:
     * - "success": (Boolean) true表示成功, false表示失败
     * - "message": (String) 操作结果的消息提示
     * - "user": (Map<String, Object>) 登录成功的用户信息 (不含密码), 如果成功 (用于Session存储)
     */
    public Map<String, Object> loginUser(String account, String password);
}
