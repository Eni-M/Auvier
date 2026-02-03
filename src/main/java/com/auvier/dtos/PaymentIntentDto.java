package com.auvier.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentIntentDto {
    private String clientSecret;
    private String paymentIntentId;
    private Long amount;
    private String currency;
    private String status;
}
