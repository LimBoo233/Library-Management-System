package com.ILoveU.service;

import com.ILoveU.dto.UserDTO;

import com.ILoveU.exception.AuthenticationException;
import com.ILoveU.exception.DuplicateResourceException;
import com.ILoveU.exception.OperationFailedException;
import com.ILoveU.exception.ValidationException;

/**
 * 用户服务接口，提供用户注册和登录功能
 */
public interface UserService {
    /**
     * 注册新用户
     * 
     * @param account 用户账号
     * @param password 用户密码
     * @param name 用户名称
     * @return 注册成功的用户信息
     * @throws ValidationException 当输入数据验证失败时抛出
     * @throws DuplicateResourceException 当用户账号已存在时抛出
     * @throws OperationFailedException 当注册操作失败时抛出
     */
    UserDTO registerUser(String account, String password, String name)
            throws ValidationException, DuplicateResourceException, OperationFailedException;

    /**
     * 用户登录
     * 
     * @param account 用户账号
     * @param password 用户密码
     * @return 登录成功的用户信息
     * @throws ValidationException 当输入数据验证失败时抛出
     * @throws AuthenticationException 当认证失败时抛出
     */
    UserDTO loginUser(String account, String password)
            throws ValidationException, AuthenticationException;
}