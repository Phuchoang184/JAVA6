package com.leika.shop.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "Categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CategoryId")
    private Integer categoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ParentId")
    private Category parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private List<Category> subCategories;

    @Column(name = "CategoryName", nullable = false, columnDefinition = "NVARCHAR(150)")
    private String categoryName;

    @Column(name = "Slug", nullable = false, unique = true, length = 200)
    private String slug;

    @Column(name = "ImageUrl", length = 500)
    private String imageUrl;

    @Column(name = "SortOrder", nullable = false)
    private int sortOrder = 0;

    @Column(name = "IsActive", nullable = false)
    private boolean isActive = true;
}
