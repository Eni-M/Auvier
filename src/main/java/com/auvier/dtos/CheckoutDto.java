package com.auvier.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutDto {

    // Shipping information
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;
    private String apartment;
    private String city;
    private String state;
    private String zipCode;
    private String country;

    // Cart items
    private List<CartItemDto> cartItems;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemDto {
        private Long variantId;
        private String productName;
        private String size;
        private String color;
        private Integer quantity;
        private BigDecimal price;
        private String imageUrl;
    }
}
