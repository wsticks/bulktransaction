package com.williams.bulktransactionservice.model.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BulkTransactionRequest {

    @NotBlank(message = "batchId is required")
    private String batchId;
    @NotEmpty(message = "transactions list must not be empty")
    @Valid
    private List<Transactions> transactions;
}
