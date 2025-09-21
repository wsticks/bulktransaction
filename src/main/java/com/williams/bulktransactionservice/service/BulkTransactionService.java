package com.williams.bulktransactionservice.service;

import com.williams.bulktransactionservice.model.request.BulkTransactionRequest;
import com.williams.bulktransactionservice.model.request.Transactions;
import com.williams.bulktransactionservice.model.response.BulkTransactionResponse;
import com.williams.bulktransactionservice.model.response.TransactionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class BulkTransactionService {

    private static final Logger logger =
            LoggerFactory.getLogger(BulkTransactionService.class);

    private final ExternalTransactionService externalTransactionService;

    public BulkTransactionService(ExternalTransactionService externalTransactionService) {
        this.externalTransactionService = externalTransactionService;
    }

    public ResponseEntity<BulkTransactionResponse> processBulkTransactions(BulkTransactionRequest request) {
        logger.info("Processing bulk transaction request with batchId: {}", request.getBatchId());
        Set<String> seen = new HashSet<>();
        for (Transactions tx : request.getTransactions()) {
            if (!seen.add(tx.getTransactionId())) {
                return ResponseEntity.badRequest().body(
                        new BulkTransactionResponse(request.getBatchId(),
                                List.of(new TransactionResult(tx.getTransactionId(), "FAILED", "Duplicate transactionId")))
                );
            }
        }
        List<CompletableFuture<TransactionResult>> futures = request.getTransactions().stream()
                .map(externalTransactionService::sendTransaction)
                .toList();
        List<TransactionResult> results = futures.stream()
                .map(CompletableFuture::join)
                .toList();
        logger.info("Completed processing bulk transaction request with batchId: {}", request.getBatchId());
        return ResponseEntity.ok(new BulkTransactionResponse(request.getBatchId(), results));
    }
}
