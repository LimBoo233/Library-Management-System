package com.ILoveU.exception;

/**
 * 这个异常通常表示一个操作因为不满足业务规则、权限不足或违反了某种策略而被明确禁止执行。
 * 操作本身可能在技术上是可行的，但业务逻辑不允许它发生。
 */
public class OperationForbiddenException extends ServiceException{
    public OperationForbiddenException(String message) {
        super(message);
    }

    public OperationForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }
}
