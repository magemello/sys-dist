package org.magemello.sys.node.protocols.ac.controller;

import org.magemello.sys.node.domain.Record;
import org.magemello.sys.node.domain.Transaction;
import org.magemello.sys.node.protocols.ac.service.ACProtocolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController()
@RequestMapping("ac")
public class ACProtocolController {

    private static final Logger log = LoggerFactory.getLogger(ACProtocolController.class);

    @Autowired
    private ACProtocolService acProtocolService;

    @PostMapping("propose")
    public ResponseEntity<String> propose(@RequestBody Transaction transaction) {
        log.info("/propose for transaction {}\n", transaction.get_ID());
        if (isAValidTransaction(transaction)) {
            if (acProtocolService.propose(transaction)) {
                return createResponse("AC 2PC Propose - Accepted transaction proposal: " + transaction.toString(), HttpStatus.OK);
            } else {
                return createResponse("AC 2PC Propose - Transaction for key: " + transaction.toString(), HttpStatus.BAD_REQUEST);
            }
        } else {
            return createResponse("AC 2PC Propose - Refused proposal for key: " + transaction.toString(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("commit/{id}")
    public ResponseEntity<String> commit(@PathVariable String id) {
        log.info("/commit for transaction {}\n", id);

        Record committedRecord = acProtocolService.commit(id);
        if (committedRecord != null) {
            return createResponse("AC 2PC Commit - Transaction executed: " + committedRecord.toString(), HttpStatus.OK);
        } else {
            return createResponse("AC 2PC Commit - Transaction id " + id + " not found", HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("rollback/{id}")
    public ResponseEntity<String> rollback(@PathVariable String id) {
        log.info("/rollback for transaction {}\n", id);

        Transaction transactionRollBack = acProtocolService.rollback(id);
        if (transactionRollBack != null) {
            return createResponse("AC 2PC Rollback - Executed: " + transactionRollBack.toString(), HttpStatus.OK);
        } else {
            return createResponse("AC 2PC Rollback - Transaction id " + id + " not found", HttpStatus.NOT_FOUND);
        }
    }


    private boolean isAValidTransaction(@RequestBody Transaction transaction) {
        return transaction != null && transaction.getKey() != null && transaction.get_ID() != null;
    }

    private ResponseEntity<String> createResponse(String message, HttpStatus status) {
        return ResponseEntity
                .status(status)
                .body(message);
    }
}
