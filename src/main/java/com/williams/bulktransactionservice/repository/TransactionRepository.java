package com.williams.bulktransactionservice.repository;

import com.williams.bulktransactionservice.model.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    boolean existsByBatchId(String batchId);

    List<Transaction> findByTransactionIdIn(List<String> transactionIds);
    long countByStatus(String status);
}

