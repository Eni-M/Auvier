package com.auvier.dtos;


import jakarta.validation.constraints.*;
import lombok.*;

@Data
public class ProductDto {
    private Long id;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Slug is required")
    private String slug;

    private String description;

    private boolean active = true;

//    @NotNull(message = "Please select a category")
//    private CategoryDto category;

    //    private List<ProductImageEntity> images = new ArrayList<>();

    //    private List<ProductVariantEntity> variants = new ArrayList<>();

    // If creating a simple product, you might include these:
    private Double price;
    private Integer stock;
}





