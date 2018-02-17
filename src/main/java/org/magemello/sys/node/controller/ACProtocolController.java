package org.magemello.sys.node.controller;

import org.magemello.sys.node.service.ACProtocolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

public class ACProtocolController {

    @Autowired
    private ACProtocolService service;
    
    @RequestMapping(value = {"/hello"}, method = RequestMethod.GET)
    public ResponseEntity<String> getAll() {

        return new ResponseEntity<>("Hello", HttpStatus.OK);
    }

}
