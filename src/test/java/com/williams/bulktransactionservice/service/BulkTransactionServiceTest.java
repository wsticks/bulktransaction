package com.williams.bulktransactionservice.service;

import com.williams.bulktransactionservice.model.entity.Transaction;
import com.williams.bulktransactionservice.model.request.BulkTransactionRequest;
import com.williams.bulktransactionservice.model.request.Transactions;
import com.williams.bulktransactionservice.model.response.BulkTransactionResponse;
import com.williams.bulktransactionservice.model.response.TransactionResult;
import com.williams.bulktransactionservice.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class BulkTransactionServiceTest {

    @Mock
    private ExternalTransactionService externalTransactionService;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private BulkTransactionService bulkTransactionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testProcessBulkTransactions_DuplicateTransaction() {
        Transactions tx1 = new Transactions();
        tx1.setTransactionId("tx1");
        tx1.setAmount(new BigDecimal(100));
        Transactions tx2 = new Transactions();
        tx2.setTransactionId("tx1");
        tx2.setAmount(new BigDecimal(200));
        BulkTransactionRequest request = new BulkTransactionRequest("batch1",List.of(tx1, tx2));

        ResponseEntity<BulkTransactionResponse> response = bulkTransactionService.processBulkTransactions(request);

        assertEquals(400, response.getStatusCodeValue());
        BulkTransactionResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("batch1", body.getBatchId());
        assertEquals(1, body.getResults().size());
        TransactionResult result = body.getResults().get(0);
        assertEquals("tx1", result.getTransactionId());
        assertEquals("FAILED", result.getStatus());
        assertEquals("Duplicate transactionId in request", result.getReason());

        verifyNoInteractions(externalTransactionService);
    }

    @Test
    void testProcessBulkTransactions_Successful() {
        Transactions tx1 = new Transactions();
        tx1.setTransactionId("tx1");
        tx1.setAmount(new BigDecimal(100));

        Transactions tx2 = new Transactions();
        tx2.setTransactionId("tx2");
        tx2.setAmount(new BigDecimal(200));

        BulkTransactionRequest request = new BulkTransactionRequest("batch1", List.of(tx1, tx2));
        request.setBatchId("batch1");
        request.setTransactions(List.of(tx1, tx2));

        when(transactionRepository.existsByBatchId("batch1")).thenReturn(false);
        when(transactionRepository.findByTransactionIdIn(anyList())).thenReturn(Collections.emptyList());
        when(externalTransactionService.sendTransaction(tx1))
                .thenReturn(CompletableFuture.completedFuture(new TransactionResult("tx1", "SUCCESS", "")));
        when(externalTransactionService.sendTransaction(tx2))
                .thenReturn(CompletableFuture.completedFuture(new TransactionResult("tx2", "SUCCESS", "")));

        ResponseEntity<BulkTransactionResponse> response = bulkTransactionService.processBulkTransactions(request);

        assertEquals(200, response.getStatusCodeValue());
        BulkTransactionResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("batch1", body.getBatchId());
        assertEquals(2, body.getResults().size());
        assertTrue(body.getResults().stream().allMatch(r -> "SUCCESS".equals(r.getStatus())));

        verify(externalTransactionService, times(2)).sendTransaction(any(Transactions.class));
        verify(transactionRepository).saveAll(anyList());
    }

    @Test
    void testProcessBulkTransactions_DuplicateBatchId() {
        Transactions tx = new Transactions();
        tx.setTransactionId("tx1");
        tx.setAmount(new BigDecimal(100));

        BulkTransactionRequest request = new BulkTransactionRequest("batch1", List.of(tx));

        when(transactionRepository.existsByBatchId("batch1")).thenReturn(true);

        ResponseEntity<BulkTransactionResponse> response = bulkTransactionService.processBulkTransactions(request);

        assertEquals(400, response.getStatusCodeValue());
        BulkTransactionResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("batch1", body.getBatchId());
        assertEquals(1, body.getResults().size());
        assertEquals("FAILED", body.getResults().get(0).getStatus());
        assertEquals("BatchId already exists", body.getResults().get(0).getReason());

        verifyNoInteractions(externalTransactionService);
        verify(transactionRepository, never()).saveAll(anyList());
    }

    @Test
    void testProcessBulkTransactions_DuplicateTransactionInRequest() {
        Transactions tx1 = new Transactions();
        tx1.setTransactionId("tx1");
        tx1.setAmount(new BigDecimal(100));

        Transactions tx2 = new Transactions();
        tx2.setTransactionId("tx1");
        tx2.setAmount(new BigDecimal(200));

        BulkTransactionRequest request = new BulkTransactionRequest("batch1", List.of(tx1, tx2));

        when(transactionRepository.existsByBatchId("batch1")).thenReturn(false);

        ResponseEntity<BulkTransactionResponse> response = bulkTransactionService.processBulkTransactions(request);

        assertEquals(400, response.getStatusCodeValue());
        BulkTransactionResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("batch1", body.getBatchId());
        assertEquals("FAILED", body.getResults().get(0).getStatus());
        assertEquals("Duplicate transactionId in request", body.getResults().get(0).getReason());

        verifyNoInteractions(externalTransactionService);
    }

    @Test
    void testProcessBulkTransactions_DuplicateTransactionInDB() {
        Transactions tx1 = new Transactions();
        tx1.setTransactionId("tx1");
        tx1.setAmount(new BigDecimal(100));

        Transactions tx2 = new Transactions();
        tx2.setTransactionId("tx1");
        tx2.setAmount(new BigDecimal(200));
        BulkTransactionRequest request = new BulkTransactionRequest("batch1", List.of(tx1, tx2));

        Transaction existing = new Transaction();
        existing.setTransactionId("tx2");

        when(transactionRepository.existsByBatchId("batch1")).thenReturn(false);
        when(transactionRepository.findByTransactionIdIn(anyList())).thenReturn(List.of(existing));

        ResponseEntity<BulkTransactionResponse> response = bulkTransactionService.processBulkTransactions(request);

        assertEquals(400, response.getStatusCodeValue());
        BulkTransactionResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(1, body.getResults().size());
        assertEquals("tx1", body.getResults().get(0).getTransactionId());
        assertEquals("FAILED", body.getResults().get(0).getStatus());
        assertEquals("Duplicate transactionId in request", body.getResults().get(0).getReason());

        verifyNoInteractions(externalTransactionService);
    }

    @Test
    void testProcessBulkTransactions_PartialFailures() {
        Transactions tx1 = new Transactions();
        tx1.setTransactionId("tx1");
        tx1.setAmount(new BigDecimal(100));

        Transactions tx2 = new Transactions();
        tx2.setTransactionId("tx2");
        tx2.setAmount(new BigDecimal(200));

        BulkTransactionRequest request = new BulkTransactionRequest("batch1", List.of(tx1, tx2));

        when(transactionRepository.existsByBatchId("batch1")).thenReturn(false);
        when(transactionRepository.findByTransactionIdIn(anyList())).thenReturn(Collections.emptyList());
        when(externalTransactionService.sendTransaction(tx1))
                .thenReturn(CompletableFuture.completedFuture(new TransactionResult("tx1", "SUCCESS", "")));
        when(externalTransactionService.sendTransaction(tx2))
                .thenReturn(CompletableFuture.completedFuture(new TransactionResult("tx2", "FAILED", "Insufficient funds")));

        ResponseEntity<BulkTransactionResponse> response = bulkTransactionService.processBulkTransactions(request);

        assertEquals(200, response.getStatusCodeValue());

        BulkTransactionResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(2, body.getResults().size());
        assertEquals("SUCCESS", body.getResults().get(0).getStatus());
        assertEquals("FAILED", body.getResults().get(1).getStatus());
        assertEquals("Insufficient funds", body.getResults().get(1).getReason());

        verify(externalTransactionService, times(2)).sendTransaction(any(Transactions.class));
        verify(transactionRepository).saveAll(anyList());
    }

    @Test
    void testProcessBulkTransactions_EmptyList() {

        BulkTransactionRequest request = new BulkTransactionRequest("batch1", Collections.emptyList());
        when(transactionRepository.existsByBatchId("batch1")).thenReturn(false);

        ResponseEntity<BulkTransactionResponse> response = bulkTransactionService.processBulkTransactions(request);

        assertEquals(200, response.getStatusCodeValue());
        BulkTransactionResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(0, body.getResults().size());

        verify(transactionRepository).saveAll(anyList());
    }
}
