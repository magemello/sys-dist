package org.magemello.sys.node.controller;

import org.magemello.sys.node.service.ProtocolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ACProtocolController {

    @Autowired
    private ProtocolService service;

    @RequestMapping(value = {"/hello" }, method = RequestMethod.GET)
    public ResponseEntity<String> getAll() {

        return new ResponseEntity<>(service.test(), HttpStatus.OK);
    }

//    @RequestMapping(value = {"/ac"}, method = RequestMethod.POST)
//    public ResponseEntity<String> getAll() {
//
//        return new ResponseEntity<>("Hello", HttpStatus.OK);
//    }
//
//
//
//    app.post("/2pc/propose", api_2pc.propose);
//    app.post("/2pc/commit/:id", api_2pc.commit);
//    app.post("/2pc/rollback/:id", api_2pc.rollback);

}
