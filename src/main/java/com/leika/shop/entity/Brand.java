package com.leika.shop.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Brands")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Brand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BrandId")
    private Integer brandId;

    @Column(name = "BrandName", nullable = false, unique = true, columnDefinition = "NVARCHAR(150)")
    private String brandName;

    @Column(name = "LogoUrl", length = 500)
    private String logoUrl;

    @Column(name = "Description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "IsActive", nullable = false)
    private boolean isActive = true;
}
