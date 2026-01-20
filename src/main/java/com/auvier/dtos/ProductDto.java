package com.auvier.dtos;


import jakarta.validation.constraints.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {
    private Long id;

    @NotBlank
    private String name;

    @NotBlank
    private String slug;

    private String description;

    private boolean active = true;

    @NotNull(message = "Please select a category")
    private CategoryDto category;

    private CategoryDto subCategory;

    private List<ProductVariantDto> variants = new ArrayList<>();
}






