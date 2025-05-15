package com.ILoveU.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Book Data Transfer Object (DTO).
 * 用于在API层传输图书信息，其结构符合API规范。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookDTO {

    private Integer id;
    private String title;
    private String isbn;
    private Integer numCopiesTotal;
    private Integer numCopiesAvailable;

    // 时间戳字段，格式为ISO8601字符串
    private String createdAt;
    private String updatedAt;

    // 关联对象的DTO列表/对象
    private List<AuthorInfoDTO> authors;
    private PressInfoDTO press;
    private List<TagInfoDTO> tags;

    // --- 嵌套DTO定义 ---
    // 这些也可以是独立的DTO文件，但作为静态内部类在这里展示更紧凑

    /**
     * 用于在BookDTO中表示作者摘要信息的嵌套DTO。
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthorInfoDTO {
        private Integer id;
        private String firstName;
        private String lastName;
        // 如果API规范中作者对象还有其他字段需要在此处显示，可以添加
    }

    /**
     * 用于在BookDTO中表示出版社摘要信息的嵌套DTO。
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PressInfoDTO {
        private Integer id;
        private String name;
    }

    /**
     * 用于在BookDTO中表示标签摘要信息的嵌套DTO。
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TagInfoDTO {
        private Integer id;
        private String name;
    }

}