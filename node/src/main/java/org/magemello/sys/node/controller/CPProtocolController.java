package org.magemello.sys.node.controller;

import org.magemello.sys.node.domain.Vote;
import org.magemello.sys.node.service.CPProtocolService;
import org.magemello.sys.protocol.raft.Raft;
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
        if (cpProtocolService.beat(update)) {
            return createResponse("CP RAFT Update - Update success: " + update.toString(), HttpStatus.OK);
        } else {
            return createResponse("CP RAFT Update - Update failed: " + update.toString(), HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("voteforme")
    public ResponseEntity<String> voteforme(@RequestBody Vote vote) {
        if (isAValidVote(vote)) {
            log.info("/vote request for {} term {}", vote.getPort(), vote.getTerm());

            if (cpProtocolService.vote(vote)) {
                return createResponse("CP RAFT Vote - Voting yes: " + vote.toString(), HttpStatus.OK);
            } else {
                return createResponse("CP RAFT Vote - Voting no: " + vote.toString(), HttpStatus.NOT_FOUND);
            }
        } else {
            return createResponse("CP RAFT Vote - Refused vote for" + vote.toString(), HttpStatus.BAD_REQUEST);
        }
    }
//
//    @GetMapping("history")
//    public ResponseEntity<List<Record>> history() {
//    }

    private boolean isAValidVote(Vote vote) {
        return vote != null && vote.getPort() != null && vote.getTerm() != null;
    }

//    private boolean isAValidTransaction(@RequestBody Transaction transaction) {
//        return transaction != null && transaction.getKey() != null && transaction.get_ID() != null;
//    }

    private ResponseEntity<String> createResponse(String message, HttpStatus status) {
        return ResponseEntity
                .status(status)
                .body(message);
    }
}
