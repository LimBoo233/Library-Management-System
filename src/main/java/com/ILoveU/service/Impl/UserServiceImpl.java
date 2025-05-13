package com.ILoveU.service.Impl;

import com.ILoveU.dao.UserDAO;
import com.ILoveU.dao.impl.UserDaoImpl;
import com.ILoveU.model.User;
import com.ILoveU.service.UserService;
import com.ILoveU.util.Log;
import com.ILoveU.util.PasswordUtil;

import java.util.HashMap;
import java.util.Map;

public class UserServiceImpl implements UserService {

    private final UserDAO userDAO;

    public UserServiceImpl() {
        this.userDAO = new UserDaoImpl();
    }

    public UserServiceImpl(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @Override
    public Map<String, Object> registerUser(String account, String password, String name) {
        Map<String, Object> result = new HashMap<>();

        // 1. 基本参数校验 (可以根据需求做得更完善)
        if (account == null || account.trim().isEmpty() ||
                password == null || password.isEmpty() || // 密码不应该只是trim().isEmpty()，而是不能为空
                name == null || name.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "账户、密码和姓名均不能为空");
            return result;
        }

        // 2. 检查账户是否已存在 (调用Hibernate版本的UserDAO)
        if (userDAO.isAccountExists(account)) {
            result.put("success", false);
            result.put("message", "账户 '" + account + "' 已被注册，请尝试其他账户。");
            return result;
        }

        // 3. 密码哈希处理 (使用PasswordUtil)
        String hashedPassword = PasswordUtil.hashPassword(password);

        // 4. 创建用户实体对象
        User newUser = new User();
        newUser.setAccount(account);
        newUser.setPassword(hashedPassword); // 存储哈希后的密码
        newUser.setName(name);

        // 5. 调用DAO将用户保存到数据库 (Hibernate版本的UserDAO)
        User createdUser = null;
        try {
            createdUser = userDAO.addUser(newUser);
        } catch (Exception e) {
            // 捕获DAO层可能抛出的与数据库相关的异常 (例如唯一约束冲突，尽管isAccountExists应该先捕获)
            e.printStackTrace(); // 实际项目中应使用日志框架记录异常
            result.put("success", false);
            result.put("message", "注册过程中发生错误，请稍后再试。");
            return result;
        }


        if (createdUser != null && createdUser.getId() > 0) {
            result.put("success", true);
            result.put("message", "用户注册成功！");
            // 返回部分用户信息给Controller层，以便后续处理 (例如存入Session或返回给前端)
            // 确保不返回密码
            Map<String, Object> userData = new HashMap<>();
            userData.put("userId", createdUser.getId());
            userData.put("account", createdUser.getAccount());
            userData.put("name", createdUser.getName());
            result.put("user", userData);
        } else {
            // 这种情况理论上如果DAO的addUser在失败时返回null或抛异常，可以被上面的catch覆盖
            // 但作为双重保险或如果DAO实现不同，可以保留
            result.put("success", false);
            result.put("message", "用户注册失败，数据库操作未成功。");
        }
        return result;
    }

    @Override
    public Map<String, Object> loginUser(String account, String password) {
        Map<String, Object> result = new HashMap<>();

        // 1. 基本参数校验
        if (account == null || account.trim().isEmpty() ||
                password == null || password.isEmpty()) {
            result.put("success", false);
            result.put("message", "账户和密码不能为空");
            return result;
        }

        // 2. 根据账户查找用户 (调用Hibernate版本的UserDAO)
        User user = null;
        try {
            user = userDAO.findUserByAccount(account);
        } catch (Exception e) {
            Log.Instance().severe("登录时查询用户信息失败：" + e.getMessage());
            result.put("success", false);
            result.put("message", "登录时查询用户信息失败，请稍后再试。");
            return result;
        }


        if (user == null) {
            result.put("success", false);
            result.put("message", "账户或密码错误。"); // 避免提示账户不存在，增加安全性
            return result;
        }

        // 3. 验证密码 (使用PasswordUtil比较用户输入的密码和数据库中存储的哈希密码)
        if (PasswordUtil.verifyPassword(password, user.getPassword())) {
            // 密码正确
            result.put("success", true);
            result.put("message", "登录成功！");

            // 准备要返回给Servlet层（进而可能存入Session）的用户信息
            // 确保不返回密码
            Map<String, Object> userData = new HashMap<>();
            userData.put("userId", user.getId());
            userData.put("name", user.getName());
            userData.put("account", user.getAccount());
            result.put("user", userData);
        } else {
            // 密码错误
            result.put("success", false);
            result.put("message", "账户或密码错误。");
        }
        return result;
    }
}
