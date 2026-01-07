package com.auvier.dtos;


import com.auvier.entities.catalog.ProductVariantEntity;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProductDto {
    private Long id;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Slug is required")
    private String slug;

    private String description;

    private boolean active = true;

   @NotNull(message = "Please select a category")
   private CategoryDto category;


    private List<ProductVariantEntity> variants = new ArrayList<>();

}





