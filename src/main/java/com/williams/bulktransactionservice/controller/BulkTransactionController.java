package com.williams.bulktransactionservice.controller;

import com.williams.bulktransactionservice.constant.AppConstant;
import com.williams.bulktransactionservice.model.request.BulkTransactionRequest;
import com.williams.bulktransactionservice.service.BulkTransactionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(AppConstant.APP_CONTENT+"/bulk-transactions")
public class BulkTransactionController {

    private static final Logger logger =
            LoggerFactory.getLogger(BulkTransactionController.class);

    private final BulkTransactionService bulkTransactionService;

    public BulkTransactionController(BulkTransactionService bulkTransactionService) {
        this.bulkTransactionService = bulkTransactionService;
    }


    @PostMapping
    public ResponseEntity<?> processBulkTransactions(@Valid @RequestBody
            BulkTransactionRequest bulkTransactionRequest){
        logger.info("ENTRY :transaction processing initiated  ");
        return bulkTransactionService.processBulkTransactions(bulkTransactionRequest);
    }

}
