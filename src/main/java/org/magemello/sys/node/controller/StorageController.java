package org.magemello.sys.node.controller;

import org.magemello.sys.node.domain.Record;
import org.magemello.sys.node.repository.RecordRepository;
import org.magemello.sys.node.service.ProtocolStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController(value="/storage")
public class StorageController {

    @Autowired
    RecordRepository recordRepository;

    @Autowired
    ProtocolStorageService protocolStorageService;

    @RequestMapping(value = {"/{key}/{value}"}, method = RequestMethod.POST)
    public ResponseEntity<?> set(){

       return new ResponseEntity<>(HttpStatus.OK);
    }
}
