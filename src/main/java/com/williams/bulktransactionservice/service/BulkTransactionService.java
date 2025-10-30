package com.williams.bulktransactionservice.service;

import com.williams.bulktransactionservice.model.entity.Transaction;
import com.williams.bulktransactionservice.model.request.BulkTransactionRequest;
import com.williams.bulktransactionservice.model.response.BulkTransactionResponse;
import com.williams.bulktransactionservice.model.response.TransactionResult;
import com.williams.bulktransactionservice.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class BulkTransactionService {

    private static final Logger logger =
            LoggerFactory.getLogger(BulkTransactionService.class);

    private final ExternalTransactionService externalTransactionService;
    private final TransactionRepository transactionRepository;


    public BulkTransactionService(ExternalTransactionService externalTransactionService, TransactionRepository transactionRepository) {
        this.externalTransactionService = externalTransactionService;
        this.transactionRepository = transactionRepository;
    }

    public ResponseEntity<BulkTransactionResponse> processBulkTransactions(BulkTransactionRequest request) {
        logger.info("Processing bulk transaction request with batchId: {}", request.getBatchId());

        // Check if batchId already exists in DB
        if (transactionRepository.existsByBatchId(request.getBatchId())) {
            logger.error("Duplicate batchId detected: {}", request.getBatchId());
            return ResponseEntity.badRequest().body(
                    new BulkTransactionResponse(request.getBatchId(),
                            List.of(new TransactionResult(null, "FAILED", "BatchId already exists")))
            );
        }
        // Check for duplicate transactionIds in the request itself
        Set<String> seenTransactionIds = new HashSet<>();
        for (var tx : request.getTransactions()) {
            if (!seenTransactionIds.add(tx.getTransactionId())) {
                logger.warn("Duplicate transactionId found in request: {}", tx.getTransactionId());
                return ResponseEntity.badRequest().body(
                        new BulkTransactionResponse(request.getBatchId(),
                                List.of(new TransactionResult(tx.getTransactionId(), "FAILED", "Duplicate transactionId in request")))
                );
            }
        }
        // Check if any transactionId already exists in DB
        List<String> txIds = request.getTransactions().stream()
                .map(t -> t.getTransactionId().trim())
                .toList();

        List<Transaction> existingTransactions = transactionRepository.findByTransactionIdIn(txIds);
        if (!existingTransactions.isEmpty()) {
            logger.warn("Duplicate transactionIds found in DB: {}", existingTransactions.stream().map(Transaction::getTransactionId).toList());
            List<TransactionResult> duplicateResults = new ArrayList<>();
            for (Transaction existing : existingTransactions) {
                duplicateResults.add(new TransactionResult(existing.getTransactionId(), "FAILED", "TransactionId already exists"));
            }
            return ResponseEntity.badRequest().body(new BulkTransactionResponse(request.getBatchId(), duplicateResults));
        }

        // Process transactions asynchronously
        List<CompletableFuture<TransactionResult>> futures = request.getTransactions().stream()
                .map(externalTransactionService::sendTransaction)
                .toList();

        List<TransactionResult> results = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        // Persist each processed transaction
        List<Transaction> entities = new ArrayList<>();
        for (TransactionResult result : results) {
            Transaction transaction = new Transaction();
            transaction.setBatchId(request.getBatchId());
            transaction.setTransactionId(result.getTransactionId());
            transaction.setStatus(result.getStatus());
            transaction.setReason(result.getReason() != null ? result.getReason() : "N/A");
            entities.add(transaction);
        }
        transactionRepository.saveAll(entities);

        logger.info("Completed processing and saved {} transactions for batchId: {}", entities.size(), request.getBatchId());

        return ResponseEntity.ok(new BulkTransactionResponse(request.getBatchId(), results));
    }

    public ResponseEntity<Map<String, Object>> getSystemMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        long totalTransactions = transactionRepository.count();
        long successfulTransactions = transactionRepository.countByStatus("SUCCESS");
        long failedTransactions = transactionRepository.countByStatus("FAILED");

        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        long uptimeMs = runtimeMXBean.getUptime();
        Duration uptime = Duration.ofMillis(uptimeMs);

        metrics.put("totalTransactions", totalTransactions);
        metrics.put("successfulTransactions", successfulTransactions);
        metrics.put("failedTransactions", failedTransactions);
        metrics.put("uptime", uptime.toMinutes() + " minutes");

        logger.info("Batch processing and System metrics result fetched successfully");
        return ResponseEntity.ok(metrics);
    }
}
