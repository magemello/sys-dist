package org.magemello.sys.node.protocols.cp.controller;

import java.util.ArrayList;

import org.magemello.sys.node.protocols.cp.domain.CPRecord;
import org.magemello.sys.node.protocols.cp.domain.Update;
import org.magemello.sys.node.protocols.cp.domain.VoteRequest;
import org.magemello.sys.node.protocols.cp.service.CPProtocolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping("cp")
public class CPProtocolController {

    @Autowired
    private CPProtocolService cpProtocolService;

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
    public ArrayList<CPRecord> history(@PathVariable Integer term, @PathVariable Integer tick) {
        return cpProtocolService.getHistory(term, tick);
    }

    private ResponseEntity<String> createResponse(String message, HttpStatus status) {
        return ResponseEntity
                .status(status)
                .body(message);
    }
}
