package org.magemello.sys.node.controller;

import org.magemello.sys.node.domain.Record;
import org.magemello.sys.node.domain.Response;
import org.magemello.sys.node.service.ACProtocolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController("ac")
public class ACProtocolController {

    @Autowired
    private ACProtocolService acProtocolService;

    @PostMapping("propose")
    public ResponseEntity<Response> propose(@RequestBody Record record) {
        if (isAValidTransaction(record)) {
            if (acProtocolService.propose(record)) {
                return createResponse("AC 2PC Propose - Accepted transaction proposal: " + record.toString(), HttpStatus.OK);
            } else {
                return createResponse("AC 2PC Propose - Transaction for key: " + record.toString(), HttpStatus.BAD_REQUEST);
            }
        } else {
            return createResponse("AC 2PC Propose - Refused proposal for key: " + record.toString(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("commit/{id}")
    public ResponseEntity<Response> commit(@PathVariable String id) {
        Record committedRecord = acProtocolService.commit(id);
        if (committedRecord != null) {
            return createResponse("AC 2PC Commit - Transaction executed: " + committedRecord.toString(), HttpStatus.OK);
        } else {
            return createResponse("AC 2PC Commit - Transaction id " + id + " not found", HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("rollback/{id}")
    public ResponseEntity<Response> rollback(@PathVariable String id) {
        Record rolledBackRecord = acProtocolService.rollback(id);
        if (rolledBackRecord != null) {
            return createResponse("AC 2PC Rollback - Executed: " + rolledBackRecord.toString(), HttpStatus.OK);
        } else {
            return createResponse("AC 2PC Rollback - Transaction id " + id + " not found", HttpStatus.BAD_REQUEST);
        }
    }

    private boolean isAValidTransaction(@RequestBody Record record) {
        return record != null && record.getKey() != null && record.get_ID() != null;
    }

    private ResponseEntity<Response> createResponse(String message, HttpStatus status) {
        return new ResponseEntity<>(new Response(message, status), status);
    }
}
