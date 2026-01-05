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

    // Explicit getters for IDE/template analyzers (Lombok generates these at compile time)
    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public String getSku() { return sku; }
    public BigDecimal getPrice() { return price; }
    public Integer getStock() { return stock; }
    public String getColor() { return color; }
    public Size getSize() { return size; }
    public boolean isActive() { return active; }
}
