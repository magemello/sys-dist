package org.magemello.sys.node.controller;

import org.magemello.sys.node.domain.Record;
import org.magemello.sys.node.service.APProtocolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController()
@RequestMapping("ap")
public class APProtocolController {

    @Autowired
    private APProtocolService apProtocolService;

    @PostMapping("propose")
    public ResponseEntity<String> propose(@RequestBody Record record) {
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
    public ResponseEntity<String> commit(@PathVariable String id) {
        Record committedRecord = apProtocolService.commit(id);
        if (committedRecord != null) {
            return createResponse("AP QUORUM Commit - Transaction executed: " + committedRecord.toString(), HttpStatus.OK);
        } else {
            return createResponse("AP QUORUM Commit - Transaction id " + id + " not found", HttpStatus.BAD_REQUEST);
        }

    }

    @PostMapping("rollback/{id}")
    public ResponseEntity<String> rollback(@PathVariable String id) {
        Record rolledBackRecord = apProtocolService.rollback(id);
        if (rolledBackRecord != null) {
            return createResponse("AP QUORUM Rollback - Executed: " + rolledBackRecord.toString(), HttpStatus.OK);
        } else {
            return createResponse("AP QUORUM Rollback - Transaction id " + id + " not found", HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("repair")
    public ResponseEntity<String> repair(@RequestBody Record record) {
        Record repairedRecord = apProtocolService.repair(record);
        return createResponse("AP QUORUM Repair - Executed: " + repairedRecord.toString(), HttpStatus.OK);
    }

    @PostMapping("read") // TODO: 25/05/2018 MAYBE WE WANT TO CHANGE THE NAME  TO VERIFY
    public ResponseEntity<String> read(@RequestBody Record record) {
        if (apProtocolService.checkValue(record)) {
            return createResponse("AP QUORUM Read - Record: " + record.toString() + " match", HttpStatus.OK);
        } else {
            return createResponse("AP QUORUM Read - Record: " + record.toString() + " miss match", HttpStatus.BAD_REQUEST);
        }
    }

    private boolean isAValidTransaction(@RequestBody Record record) {
        return record != null && record.getKey() != null && record.get_ID() != null;
    }

    private ResponseEntity<String> createResponse(String message, HttpStatus status) {
        return ResponseEntity
                .status(status)
                .body(message);
    }
}
