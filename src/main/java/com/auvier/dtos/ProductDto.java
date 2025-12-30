package com.auvier.dtos;


import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {

    @PositiveOrZero(message = "Id must be 0 or positive")
    private Long id;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 50, message = "Name must be 2 to 50 characters")
    private String name;

    @NotBlank(message = "Slug is required")
    @Size(min = 2, max = 80, message = "Slug must be 2 to 80 characters")
    @Pattern(
            regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
            message = "Slug must be lowercase words separated by single hyphens (no spaces)"
    )
    private String slug;

    @NotBlank(message = "Description is required")
    @Size(min = 20, max = 2000, message = "Description must be 20 to 2000 characters")
    private String description;

    private boolean active = true;

    @NotNull(message = "Category is required")
    @Positive(message = "Category id must be positive")
    private Long categoryId;

    @NotNull(message = "User is required")
    @Positive(message = "User id must be positive")
    private Long userId;

    //    private CategoryEntity category;
//
//    private List<ProductImageEntity> images = new ArrayList<>();
//
//    private List<ProductVariantEntity> variants = new ArrayList<>();
}

