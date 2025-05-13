package com.ILoveU.service.Impl;

import com.ILoveU.dao.UserDAO;
import com.ILoveU.dao.impl.UserDaoImpl;
import com.ILoveU.dto.UserDTO;
import com.ILoveU.exception.AuthenticationException;
import com.ILoveU.exception.DuplicateResourceException;
import com.ILoveU.exception.OperationFailedException;
import com.ILoveU.exception.ValidationException;
import com.ILoveU.model.User;
import com.ILoveU.service.UserService;
import com.ILoveU.util.PasswordUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    private final UserDAO userDAO;

    public UserServiceImpl() {
        this.userDAO = new UserDaoImpl(); // 或者通过构造函数注入
    }

    // 用于测试的构造函数
    public UserServiceImpl(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @Override
    public UserDTO registerUser(String account, String password, String name)
            throws ValidationException, DuplicateResourceException, OperationFailedException {

        // 1. 基本参数校验
        if (account == null || account.trim().isEmpty() ||
                password == null || password.isEmpty() ||
                name == null || name.trim().isEmpty()) {
            // 如果需要字段级错误，可以构造一个List<FieldErrorDetail>
            // List<ApiErrorResponse.FieldErrorDetail> errors = new ArrayList<>();
            // if (account == null || account.trim().isEmpty()) errors.add(new ApiErrorResponse.FieldErrorDetail("account", "账户不能为空"));
            // ...
            // throw new ValidationException("输入参数无效", errors);
            throw new ValidationException("账户、密码和姓名均不能为空");
        }

        // 2. 检查账户是否已存在
        if (userDAO.isAccountExists(account)) {
            throw new DuplicateResourceException("账户 '" + account + "' 已被注册，请尝试其他账户。");
        }

        // 3. 密码哈希处理
        String hashedPassword = PasswordUtil.hashPassword(password);

        // 4. 创建用户实体对象
        User newUser = new User();
        newUser.setAccount(account);
        newUser.setPassword(hashedPassword);
        newUser.setName(name); // 假设User实体类有setName方法

        // 5. 调用DAO将用户保存到数据库
        User createdUser;
        try {
            createdUser = userDAO.addUser(newUser);
        } catch (Exception e) {
            // 捕获DAO层可能抛出的与数据库相关的异常
            logger.error("用户注册时数据库操作失败: {}", e.getMessage(), e);
            throw new OperationFailedException("注册过程中发生数据库错误，请稍后再试。", e);
        }

        if (createdUser == null || createdUser.getId() <= 0) { // 假设User实体有getId()方法
            logger.error("用户注册后未能获取有效的用户信息。");
            throw new OperationFailedException("用户注册失败，未能成功保存用户信息。");
        }

        // 注册成功，返回UserDTO
        // API规范中注册成功返回的 "username" 对应我们User实体的 "name"
        logger.info("用户 {} 注册成功, ID: {}", createdUser.getAccount(), createdUser.getId());
        return new UserDTO(createdUser.getId(), createdUser.getName(), createdUser.getAccount());
    }

    @Override
    public UserDTO loginUser(String account, String password)
            throws ValidationException, AuthenticationException {

        // 1. 基本参数校验
        if (account == null || account.trim().isEmpty() ||
                password == null || password.isEmpty()) {
            throw new ValidationException("账户和密码不能为空");
        }

        // 2. 根据账户查找用户
        User user;
        try {
            user = userDAO.findUserByAccount(account);
        } catch (Exception e) {
            logger.error("登录时查询用户信息失败: {}", e.getMessage(), e);
            // 可以考虑包装成特定的数据访问异常，或者直接认为是认证流程的一部分
            throw new AuthenticationException("登录处理失败，请稍后再试。");
        }

        if (user == null) {
            logger.warn("尝试登录失败，账户不存在: {}", account);
            throw new AuthenticationException("账户或密码错误。");
        }

        // 3. 验证密码
        if (PasswordUtil.verifyPassword(password, user.getPassword())) {
            // 密码正确，登录成功
            logger.info("用户 {} 登录成功, ID: {}", user.getAccount(), user.getId());
            // API规范中登录成功返回的 "user.username" 对应我们User实体的 "name"
            return new UserDTO(user.getId(), user.getName(), user.getAccount());
        } else {
            // 密码错误
            logger.warn("尝试登录失败，密码错误，账户: {}", account);
            throw new AuthenticationException("账户或密码错误。");
        }
    }
}
