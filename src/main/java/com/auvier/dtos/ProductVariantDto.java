package com.auvier.dtos;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.auvier.enums.Size;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantDto {

    @PositiveOrZero
    private Long id;

    @NotNull
    @Positive
    private Long productId;

    @NotBlank
    private String sku;

    @NotNull
    @Positive
    private BigDecimal price;

    @NotNull
    @PositiveOrZero
    private Integer stock;

    @NotBlank
    private String color;

    @NotNull
    private Size size;

    private boolean active = true;
}
