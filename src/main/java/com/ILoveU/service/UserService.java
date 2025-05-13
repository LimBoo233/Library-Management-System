package com.ILoveU.service;

import com.ILoveU.dto.UserDTO;

import com.ILoveU.exception.AuthenticationException;
import com.ILoveU.exception.DuplicateResourceException;
import com.ILoveU.exception.OperationFailedException;
import com.ILoveU.exception.ValidationException;

public interface UserService {
    UserDTO registerUser(String account, String password, String name)
            throws ValidationException, DuplicateResourceException, OperationFailedException;

    UserDTO loginUser(String account, String password)
            throws ValidationException, AuthenticationException;
}