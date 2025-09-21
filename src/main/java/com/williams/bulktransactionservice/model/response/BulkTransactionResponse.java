package com.williams.bulktransactionservice.model.response;

import lombok.Data;

import java.util.List;

@Data
public class BulkTransactionResponse {

    private String batchId;
    private List<TransactionResult> results;

    public BulkTransactionResponse(String batchId, List<TransactionResult> results) {
        this.batchId = batchId;
        this.results = results;
    }

}
