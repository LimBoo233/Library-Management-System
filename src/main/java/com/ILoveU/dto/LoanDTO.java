package com.ILoveU.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoanDTO {
    private Integer id;
    private Integer userId;
    private Integer bookId;

    // 格式为ISO8601字符串
    // ex: 2025-05-12T17:44:00Z
    private String checkoutDate;
    private String dueDate;
    private String returnDate;

    private Boolean isOverdue;
}
