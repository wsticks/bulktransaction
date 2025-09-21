package com.williams.bulktransactionservice.service;

import com.williams.bulktransactionservice.model.request.Transactions;
import com.williams.bulktransactionservice.model.response.TransactionResult;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CompletableFuture;

@Service
public class ExternalTransactionService {

    private static final Logger logger =
            LoggerFactory.getLogger(ExternalTransactionService.class);

    private final RestTemplate restTemplate;
    private final Counter successCounter;
    private final Counter failureCounter;
    @Value("${txn.service.url}")
    private String transactionServiceUrl;

    public ExternalTransactionService(RestTemplate restTemplate, MeterRegistry meterRegistry) {
        this.restTemplate = restTemplate;
        this.successCounter = meterRegistry.counter("transactions.success");
        this.failureCounter = meterRegistry.counter("transactions.failed");
    }

    @Retry(name = "transactionServiceRetry")
    @CircuitBreaker(name = "transactionServiceCB", fallbackMethod = "fallback")
    @TimeLimiter(name = "transactionServiceTimeout")
    public CompletableFuture<TransactionResult> sendTransaction(Transactions tx) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Sending transaction with transactionId: {}", tx.getTransactionId());
                ResponseEntity<Void> response = restTemplate.postForEntity(transactionServiceUrl, tx, Void.class);
                logger.info("Response from external transaction service : {}", response);
                if (response.getStatusCode().is2xxSuccessful()) {
                    successCounter.increment();
                    return new TransactionResult(tx.getTransactionId(), "SUCCESS", null);
                } else {
                    failureCounter.increment();
                    return new TransactionResult(tx.getTransactionId(), "FAILED",
                            "Transaction service returned: " + response.getStatusCode());
                }
            } catch (HttpClientErrorException | HttpServerErrorException ex) {
                failureCounter.increment();
                return new TransactionResult(tx.getTransactionId(), "FAILED", ex.getResponseBodyAsString());
            } catch (Exception ex) {
                failureCounter.increment();
                return new TransactionResult(tx.getTransactionId(), "FAILED", "Unexpected error");
            }
        });
    }

    public CompletableFuture<TransactionResult> fallback(Transactions tx, Throwable ex) {
        failureCounter.increment();
        return CompletableFuture.completedFuture(new TransactionResult(
                tx.getTransactionId(), "FAILED", "Fallback: " + ex.getMessage()
        ));
    }
}
