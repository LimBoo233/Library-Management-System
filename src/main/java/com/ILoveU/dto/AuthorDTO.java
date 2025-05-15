package com.ILoveU.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthorDTO {
    private Integer id;
    private String firstName;
    private String lastName;
    private String bio;

    /**
     * 作者记录的创建时间，格式为ISO8601字符串。
     * 对应API规范中的 "createdAt" 字段。
     * 例如: "2025-05-12T17:44:00Z"
     */
    private String createdAt;

    /**
     * 作者记录的最后更新时间，格式为ISO8601字符串。
     * 对应API规范中的 "updatedAt" 字段。
     * 例如: "2025-05-12T17:44:00Z"
     */
    private String updatedAt;
}
