package org.magemello.sys.node.protocols.cp.controller;

import org.magemello.sys.node.domain.RecordTerm;
import org.magemello.sys.node.domain.VoteRequest;
import org.magemello.sys.node.protocols.cp.domain.Update;
import org.magemello.sys.node.protocols.cp.service.CPProtocolService;
import org.magemello.sys.node.repository.RecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

@RestController()
@RequestMapping("cp")
public class CPProtocolController {

    private static final Logger log = LoggerFactory.getLogger(CPProtocolController.class);

    @Autowired
    private CPProtocolService cpProtocolService;

    @Autowired
    private RecordRepository recordRepository;

    @PostMapping("update")
    public ResponseEntity<String> update(@RequestBody Update update) {
        ResponseEntity<String> res;
        if (cpProtocolService.handleBeat(update)) {
            res = createResponse("CP RAFT Update - Update success: " + update.toString(), HttpStatus.OK);
        } else {
            res = createResponse("CP RAFT Update - Update failed: " + update.toString(), HttpStatus.NOT_FOUND);
        }

        return res;
    }

    @PostMapping("voteforme")
    public ResponseEntity<String> voteforme(@RequestBody VoteRequest vote) {
        ResponseEntity<String> res;
        if (cpProtocolService.handleVoteRequest(vote)) {
            res = createResponse("CP RAFT Vote - Voting yes: " + vote.toString(), HttpStatus.OK);
        } else {
            res = createResponse("CP RAFT Vote - Voting no: " + vote.toString(), HttpStatus.NOT_FOUND);
        }

        return res;
    }



    @GetMapping("history/{term}/{tick}")
    public ArrayList<RecordTerm> history(@PathVariable Integer term, @PathVariable Integer tick) {
        return recordRepository.findByTermLessThanEqualAndTickLessThanEqual(term, tick);
    }

    private ResponseEntity<String> createResponse(String message, HttpStatus status) {
        return ResponseEntity
                .status(status)
                .body(message);
    }
}
