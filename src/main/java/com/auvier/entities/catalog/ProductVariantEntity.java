package com.auvier.entities.catalog;

import com.auvier.enums.Size;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity(name = "product_variants")
@Getter
@Setter
@NoArgsConstructor
public class ProductVariantEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(nullable = false, unique = true, length = 60)
    private String sku;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stock;

    @Column(nullable = false, length = 30)
    private String color;


    private Size size;

    @Column(nullable = false)
    private boolean active = true;
}
