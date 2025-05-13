package com.ILoveU.exception;

import com.ILoveU.dto.ApiErrorResponse.FieldErrorDetail;
import lombok.Getter;

import java.util.List;
import java.util.Collections;

@Getter
public class ValidationException extends ServiceException {
    private final List<FieldErrorDetail> errors;

    public ValidationException(String message) {
        super(message);
        this.errors = Collections.emptyList();
    }

    public ValidationException(String message, List<FieldErrorDetail> errors) {
        super(message);
        this.errors = errors != null ? errors : Collections.emptyList();
    }


}