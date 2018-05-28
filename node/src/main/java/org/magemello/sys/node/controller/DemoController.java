package org.magemello.sys.node.controller;

import org.magemello.sys.node.domain.Record;
import org.magemello.sys.node.repository.RecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping("/demo/")
public class DemoController {

    private static final Logger log = LoggerFactory.getLogger(DemoController.class);

    @Autowired
    RecordRepository recordRepository;

    @GetMapping("/dump")
    public ResponseEntity<?> dumpDatabase() {
        log.info("===========================");
        log.info("Current database contents");
        Iterable<Record> records = recordRepository.findAll();
        for (Record record : records) {
            log.info("- {}={}", record.getKey(), record.getValue());
        }
        log.info("");

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/clean")
    public ResponseEntity<?> cleanScreen() {
        for(int i=0; i<100; i++) {
            log.info("");
        }
        
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
