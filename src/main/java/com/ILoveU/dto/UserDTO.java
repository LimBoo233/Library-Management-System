package com.ILoveU.dto;


import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class UserDTO {
    private Integer id;        // 对应API规范中的 "id"
    private String username;  // 对应API规范中的 "username" (通常映射到User实体的name字段)
    private String account;   // 对应API规范中的 "account"
}
