// 建议放在项目的 com.example.dto 包下
package com.ILoveU.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class PageDTO<T> {
    private List<T> data;
    private PaginationInfo pagination;

    // 构造函数, Getters and Setters

    public PageDTO(List<T> data, long totalItems, int currentPage, int pageSize) {
        this.data = data;
        this.pagination = new PaginationInfo(currentPage, pageSize, totalItems);
    }

    // 静态内部类用于封装分页元数据
    @Getter
    public static class PaginationInfo {
        private int currentPage;
        private int pageSize;
        private int totalPages;
        private long totalItems;

        public PaginationInfo(int currentPage, int pageSize, long totalItems) {
            this.currentPage = currentPage;
            this.pageSize = pageSize;
            this.totalItems = totalItems;
            this.totalPages = (pageSize == 0 || totalItems == 0) ? 1 : (int) Math.ceil((double) totalItems / pageSize);
            if (this.totalPages == 0 && totalItems > 0) this.totalPages = 1; // 至少1页如果条目>0
            if (this.totalPages == 0 && totalItems == 0) this.totalPages = 0; // 如果没有条目，总页数为0
        }
    }

}