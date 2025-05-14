package com.ILoveU.dto;

import java.time.OffsetDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class ApiErrorResponse {

    private String timestamp;
    private int status;
    private String error;
    private String message;
    private String path;

    // 可选的字段级错误列表
    private List<FieldErrorDetail> errors;

    // 构造函数
    public ApiErrorResponse(int status, String error, String message, String path) {
        this.timestamp = OffsetDateTime.now(java.time.ZoneOffset.UTC).toString(); // 或者使用 DateTimeFormatter.ISO_OFFSET_DATE_TIME
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    public ApiErrorResponse(int status, String error, String message, String path, List<FieldErrorDetail> errors) {
        this(status, error, message, path); // 调用上面的构造函数
        this.errors = errors;
    }


    // 内部类用于表示字段级别的错误详情
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldErrorDetail {
        private String field;
        private String message;
    }

}