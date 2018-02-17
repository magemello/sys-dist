package org.magemello.sys.node.controller;

import org.magemello.sys.node.repository.RecordRepository;
import org.magemello.sys.node.service.ProtocolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping("/storage/")
public class StorageController {

    @Autowired
    ProtocolService protocolService;

    @Autowired
    RecordRepository recordRepository;

    @RequestMapping(value = {"/{key}/{value}"}, method = RequestMethod.POST)
    public ResponseEntity<?> set(@PathVariable String key, @PathVariable String value) {

        try {
            protocolService.set(key, value);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(e.getStackTrace(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = {"/{key}"}, method = RequestMethod.GET)
    public ResponseEntity<?> get(@PathVariable String key) {

        return new ResponseEntity<>(protocolService.get(key), HttpStatus.OK);
    }

    @RequestMapping(value = {"/dump"}, method = RequestMethod.GET)
    public ResponseEntity<?> getAll() {

        return new ResponseEntity<>(recordRepository.findAll(), HttpStatus.OK);
    }
}
