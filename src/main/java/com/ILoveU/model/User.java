package com.ILoveU.model;

import javax.persistence.*;

@Entity // 标识此类为Hibernate实体类
@Table(name = "Users") // 指定对应的数据库表名
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

    public User() {}

    public User(int id, String name, String account, String password) {
        this.id = id;
        this.name = name;
        this.account = account;
        this.password = password;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
