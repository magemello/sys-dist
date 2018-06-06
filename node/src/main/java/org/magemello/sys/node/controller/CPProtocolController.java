package org.magemello.sys.node.controller;

import org.magemello.sys.node.domain.VoteRequest;
import org.magemello.sys.node.service.CPProtocolService;
import org.magemello.sys.protocol.raft.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController()
@RequestMapping("cp")
public class CPProtocolController {

    private static final Logger log = LoggerFactory.getLogger(CPProtocolController.class);

    @Autowired
    private CPProtocolService cpProtocolService;

    @PostMapping("update")
    public ResponseEntity<String> update(@RequestBody Update update) {
        ResponseEntity<String> res;
        if (cpProtocolService.beat(update)) {
            res = createResponse("CP RAFT Update - Update success: " + update.toString(), HttpStatus.OK);
        } else {
            res =  createResponse("CP RAFT Update - Update failed: " + update.toString(), HttpStatus.NOT_FOUND);
        }

        log.info("/update {}: {} ", update.toString(), asOkay(res, "good", "fail"));
        return res;
    }

    @PostMapping("voteforme")
    public ResponseEntity<String> voteforme(@RequestBody VoteRequest vote) {
        ResponseEntity<String> res;
        if (cpProtocolService.vote(vote)) {
            res = createResponse("CP RAFT Vote - Voting yes: " + vote.toString(), HttpStatus.OK);
        } else {
            res = createResponse("CP RAFT Vote - Voting no: " + vote.toString(), HttpStatus.NOT_FOUND);
        }

        log.info("/vote request from {}, term {}: {} ", vote.getPort(), vote.getTerm(), asOkay(res, "yes", "no"));
        return res;
    }

    private String asOkay(ResponseEntity<String> res, String okay, String fail) {
        return res.getStatusCode() == HttpStatus.OK ? okay : fail;
    }

//
//    @GetMapping("history")
//    public ResponseEntity<List<Record>> history() {
//    }

    private ResponseEntity<String> createResponse(String message, HttpStatus status) {
        return ResponseEntity
                .status(status)
                .body(message);
    }
}
