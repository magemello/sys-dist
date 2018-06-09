package org.magemello.sys.node.protocols.ap.controller;

import org.magemello.sys.node.protocols.ac.domain.Transaction;
import org.magemello.sys.node.protocols.ap.domain.APRecord;
import org.magemello.sys.node.protocols.ap.service.APProtocolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController()
@RequestMapping("ap")
public class APProtocolController {

    private static final Logger log = LoggerFactory.getLogger(APProtocolController.class);

    @Autowired
    private APProtocolService apProtocolService;

    @PostMapping("propose")
    public ResponseEntity<String> propose(@RequestBody Transaction transaction) {
        log.info("\n/propose for transaction {}", transaction.get_ID());
        if (isAValidTransaction(transaction)) {
            if (apProtocolService.propose(transaction)) {
                return createResponse("AP QUORUM Propose - Accepted transaction proposal: " + transaction.toString(), HttpStatus.OK);
            } else {
                return createResponse("AP QUORUM Propose - Transaction for key: " + transaction.toString(), HttpStatus.BAD_REQUEST);
            }
        } else {
            return createResponse("AP QUORUM Propose - Refused proposal for key: " + transaction.toString(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("commit/{id}")
    public ResponseEntity<String> commit(@PathVariable String id) {
        log.info("\n/commit for transaction {}", id);
        APRecord committedRecord = apProtocolService.commit(id);
        if (committedRecord != null) {
            return createResponse("AP QUORUM Commit - Transaction executed: " + committedRecord.toString(), HttpStatus.OK);
        } else {
            return createResponse("AP QUORUM Commit - Transaction id " + id + " not found", HttpStatus.NOT_FOUND);
        }

    }

    @PostMapping("rollback/{id}")
    public ResponseEntity<String> rollback(@PathVariable String id) {
        log.info("\n/rollback for transaction {}", id);
        Transaction transactionRollBack = apProtocolService.rollback(id);
        if (transactionRollBack != null) {
            return createResponse("AP QUORUM Rollback - Executed: " + transactionRollBack.toString(), HttpStatus.OK);
        } else {
            return createResponse("AP QUORUM Rollback - Transaction id " + id + " not found", HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("repair")
    public ResponseEntity<String> repair(@RequestBody APRecord record) {
        log.info("\n/repair for key {}", record.getKey());
        APRecord repairedRecord = apProtocolService.repair(record);
        return createResponse("AP QUORUM Repair - Executed: " + repairedRecord.toString(), HttpStatus.OK);
    }

    @GetMapping("read/{key}")
    public ResponseEntity<APRecord> read(@PathVariable String key) {
        log.info("\n/read for key {}", key);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(apProtocolService.read(key));

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
