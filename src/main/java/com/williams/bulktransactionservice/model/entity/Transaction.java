package com.williams.bulktransactionservice.model.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "transaction")
@Data
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String batchId;
    private String transactionId;
    private String status;
    private String reason;

}
