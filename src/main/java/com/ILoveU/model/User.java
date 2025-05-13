package com.ILoveU.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity // 标识此类为Hibernate实体类
@Table(name = "users") // 指定对应的数据库表名
public class User {
    @Id // 标识该字段为主键
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 主键生成策略为自增
    @Column(name = "id") // 映射到 'user_id' 列
    private int id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "account", nullable = false, unique = true, length = 100) // 映射到 'account' 列，非空，唯一，长度100
    private String account;

    @Column(name = "password", nullable = false, length = 255) // 映射到 'password' 列
    private String password;
}
