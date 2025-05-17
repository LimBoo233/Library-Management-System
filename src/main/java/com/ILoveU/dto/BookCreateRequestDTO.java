package com.ILoveU.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookCreateRequestDTO {
    private String title;
    private String isbn;
    private Integer numCopiesAvailable;
    private List<Integer> authorIds;
    private Integer pressId;
    private List<Integer> tagIds;
}