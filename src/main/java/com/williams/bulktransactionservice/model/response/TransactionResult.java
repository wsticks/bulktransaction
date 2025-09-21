package com.williams.bulktransactionservice.model.response;

import lombok.Data;

@Data
public class TransactionResult {

    private String transactionId;
    private String status;
    private String reason;

    public TransactionResult(String transactionId, String status, String reason) {
        this.transactionId = transactionId;
        this.status = status;
        this.reason = reason;
    }

}