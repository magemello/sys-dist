package org.magemello.sys.node.controller;

import org.magemello.sys.node.domain.Record;
import org.magemello.sys.node.service.APProtocolService;
import org.magemello.sys.node.service.CPProtocolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController()
@RequestMapping("cp")
public class CPProtocolController {

//    @Autowired
//    private CPProtocolService cpProtocolService;
//
//    @PostMapping("update")
//    public ResponseEntity<String> update(@RequestBody Record record) {
//
//    }
//
//    @PostMapping("voteforme")
//    public ResponseEntity<String> voteforme(@RequestBody Record record) {
//
//    }
//
//    @GetMapping("history")
//    public ResponseEntity<Record> history() {
//    }

//    private boolean isAValidTransaction(@RequestBody Record record) {
//        return record != null && record.getKey() != null && record.get_ID() != null;
//    }

    private ResponseEntity<String> createResponse(String message, HttpStatus status) {
        return ResponseEntity
                .status(status)
                .body(message);
    }
}
