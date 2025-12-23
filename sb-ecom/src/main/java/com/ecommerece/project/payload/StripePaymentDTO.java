package com.ecommerece.project.payload;

import com.ecommerece.project.model.Address;

import lombok.Data;

import java.util.Map;

@Data
public class StripePaymentDTO {
    private Long amount;
    private String currency;
    private String name;
    private String email;
    private String description;
    private Address address;
    private Map<String,String> metadata;
}
