package com.williams.bulktransactionservice.service;

import com.williams.bulktransactionservice.model.request.BulkTransactionRequest;
import com.williams.bulktransactionservice.model.request.Transactions;
import com.williams.bulktransactionservice.model.response.BulkTransactionResponse;
import com.williams.bulktransactionservice.model.response.TransactionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class BulkTransactionServiceTest {

    @Mock
    private ExternalTransactionService externalTransactionService;

    @InjectMocks
    private BulkTransactionService bulkTransactionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testProcessBulkTransactions_Successful() {
        // Arrange
        Transactions tx1 = new Transactions();
        tx1.setTransactionId("tx1");
        tx1.setAmount(new BigDecimal(100));
        Transactions tx2 = new Transactions();
        tx2.setTransactionId("tx2");
        tx2.setAmount(new BigDecimal(200));
        BulkTransactionRequest request = new BulkTransactionRequest();
        request.setBatchId("batch1");
        request.setTransactions(List.of(tx1, tx2));

        when(externalTransactionService.sendTransaction(tx1))
                .thenReturn(CompletableFuture.completedFuture(new TransactionResult("tx1", "SUCCESS", "")));
        when(externalTransactionService.sendTransaction(tx2))
                .thenReturn(CompletableFuture.completedFuture(new TransactionResult("tx2", "SUCCESS", "")));

        // Act
        ResponseEntity<BulkTransactionResponse> response = bulkTransactionService.processBulkTransactions(request);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        BulkTransactionResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("batch1", body.getBatchId());
        assertEquals(2, body.getResults().size());
        assertTrue(body.getResults().stream().allMatch(r -> "SUCCESS".equals(r.getStatus())));

        verify(externalTransactionService).sendTransaction(tx1);
        verify(externalTransactionService).sendTransaction(tx2);
    }

    @Test
    void testProcessBulkTransactions_DuplicateTransaction() {
        // Arrange
        Transactions tx1 = new Transactions();
        tx1.setTransactionId("tx1");
        tx1.setAmount(new BigDecimal(100));
        Transactions tx2 = new Transactions();
        tx2.setTransactionId("tx1");
        tx2.setAmount(new BigDecimal(200));
        BulkTransactionRequest request = new BulkTransactionRequest();
        request.setBatchId("batch1");
        request.setTransactions(List.of(tx1, tx2));

        // Act
        ResponseEntity<BulkTransactionResponse> response = bulkTransactionService.processBulkTransactions(request);

        // Assert
        assertEquals(400, response.getStatusCodeValue());
        BulkTransactionResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("batch1", body.getBatchId());
        assertEquals(1, body.getResults().size());
        TransactionResult result = body.getResults().get(0);
        assertEquals("tx1", result.getTransactionId());
        assertEquals("FAILED", result.getStatus());
        assertEquals("Duplicate transactionId", result.getReason());

        // Ensure external service is never called due to early duplicate detection
        verifyNoInteractions(externalTransactionService);
    }

    @Test
    void testProcessBulkTransactions_PartialFailures() {
        // Arrange
        Transactions tx1 = new Transactions();
        tx1.setTransactionId("tx1");
        tx1.setAmount(new BigDecimal(100));
        Transactions tx2 = new Transactions();
        tx2.setTransactionId("tx2");
        tx2.setAmount(new BigDecimal(200));
        BulkTransactionRequest request = new BulkTransactionRequest();
        request.setBatchId("batch1");
        request.setTransactions(List.of(tx1, tx2));

        when(externalTransactionService.sendTransaction(tx1))
                .thenReturn(CompletableFuture.completedFuture(new TransactionResult("tx1", "SUCCESS", "")));
        when(externalTransactionService.sendTransaction(tx2))
                .thenReturn(CompletableFuture.completedFuture(new TransactionResult("tx2", "FAILED", "Insufficient funds")));

        // Act
        ResponseEntity<BulkTransactionResponse> response = bulkTransactionService.processBulkTransactions(request);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        BulkTransactionResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(2, body.getResults().size());
        assertEquals("SUCCESS", body.getResults().get(0).getStatus());
        assertEquals("FAILED", body.getResults().get(1).getStatus());
        assertEquals("Insufficient funds", body.getResults().get(1).getReason());

        verify(externalTransactionService).sendTransaction(tx1);
        verify(externalTransactionService).sendTransaction(tx2);
    }
}
