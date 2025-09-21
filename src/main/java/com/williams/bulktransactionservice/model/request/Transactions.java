package com.williams.bulktransactionservice.model.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class Transactions {

    @NotBlank(message = "transaction ID is required")
    private String transactionId;
    @NotBlank(message = "fromAccount is required")
    private String fromAccount;
    @NotBlank(message = "toAccount is required")
    private String toAccount;
    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be greater than 0")
    private BigDecimal amount;
}