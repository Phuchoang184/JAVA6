package com.leika.shop.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryDto {
    private Integer categoryId;
    private String categoryName;
    private String slug;
    private String imageUrl;
    private List<CategoryDto> subCategories;
}
