package org.magemello.sys.node.controller;

import org.magemello.sys.node.domain.Record;
import org.magemello.sys.node.domain.Response;
import org.magemello.sys.node.service.ACProtocolService;
import org.magemello.sys.node.service.APProtocolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController("ap")
public class APProtocolController {

    @Autowired
    private APProtocolService apProtocolService;

    @PostMapping("propose")
    public ResponseEntity<Response> propose(@RequestBody Record record) {
        if (isAValidTransaction(record)) {
            if (apProtocolService.propose(record)) {
                return createResponse("AP QUORUM Propose - Accepted transaction proposal: " + record.toString(), HttpStatus.OK);
            } else {
                return createResponse("AP QUORUM Propose - Transaction for key: " + record.toString(), HttpStatus.BAD_REQUEST);
            }
        } else {
            return createResponse("AP QUORUM Propose - Refused proposal for key: " + record.toString(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("commit/{id}")
    public ResponseEntity<Response> commit(@PathVariable String id) {
        Record committedRecord = apProtocolService.commit(id);
        if (committedRecord != null) {
            return createResponse("AP QUORUM Commit - Transaction executed: " + committedRecord.toString(), HttpStatus.OK);
        } else {
            return createResponse("AP QUORUM Commit - Transaction id " + id + " not found", HttpStatus.BAD_REQUEST);
        }

    }

    @PostMapping("rollback/{id}")
    public ResponseEntity<Response> rollback(@PathVariable String id) {
        Record rolledBackRecord = apProtocolService.rollback(id);
        if (rolledBackRecord != null) {
            return createResponse("AP QUORUM Rollback - Executed: " + rolledBackRecord.toString(), HttpStatus.OK);
        } else {
            return createResponse("AP QUORUM Rollback - Transaction id " + id + " not found", HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("repair/")
    public ResponseEntity<Response> repair(@RequestBody Record record) {
        Record repairedRecord = apProtocolService.repair(record);
        return createResponse("AP QUORUM Repair - Executed: " + repairedRecord.toString(), HttpStatus.OK);
    }

    @GetMapping("read/{key}")
    public ResponseEntity<Response> read(@PathVariable String key) {
        Record record = apProtocolService.get(key);
        return createResponse(record.toString(), HttpStatus.OK);
    }


    private boolean isAValidTransaction(@RequestBody Record record) {
        return record != null && record.getKey() != null && record.get_ID() != null;
    }

    private ResponseEntity<Response> createResponse(String message, HttpStatus status) {
        return new ResponseEntity<>(new Response(message, status), status);
    }
}
