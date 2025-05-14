package com.ILoveU.dao;

import com.ILoveU.exception.OperationFailedException;

public interface BookDAO {

    long countBooksByPressId(int pressId) throws OperationFailedException;
}
