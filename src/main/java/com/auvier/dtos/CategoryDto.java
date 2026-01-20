package com.auvier.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDto {

    @PositiveOrZero
    private Long id;

    @NotBlank(message = "Name is required")
    @Size(max = 25, message = "Full Name must be under 25 characters")
    private String name;

    @NotBlank(message = "Slug is required")
    @Size(max = 25, message = "Slug must be under 25 characters")
    private String slug;

    // null means root category
    private Long parentId;

    private boolean active = true;

    @Size(max = 1000, message = "Description must be under 1000 characters")
    private String description;
}

