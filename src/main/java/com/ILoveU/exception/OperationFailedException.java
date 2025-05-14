package com.ILoveU.exception;

/**
 * 这个异常通常表示一个被允许的操作在执行过程中因为某些意外的技术原因或系统问题而失败了。
 * 它更多地指向执行层面的问题，而不是业务规则层面的禁止。
 */
public class OperationFailedException extends ServiceException {
    public OperationFailedException(String message) {
        super(message);
    }

    public OperationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}