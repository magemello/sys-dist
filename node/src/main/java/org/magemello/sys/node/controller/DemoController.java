package org.magemello.sys.node.controller;

import org.magemello.sys.node.domain.Record;
import org.magemello.sys.node.repository.RecordRepository;
import org.magemello.sys.node.service.ProtocolServiceProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

@RestController()
@RequestMapping("/demo/")
public class DemoController {

    private static final Logger log = LoggerFactory.getLogger(DemoController.class);

    @Autowired
    RecordRepository recordRepository;

    @Autowired
    ProtocolServiceProxy protocolService;

    @GetMapping("/dump")
    public ResponseEntity<?> dumpDatabase() throws JsonProcessingException {

        ObjectWriter writer = new ObjectMapper()
                .writer()
                .withDefaultPrettyPrinter();

        log.info("\n\n===========================");
        log.info("\nCurrent database contents:");
        Iterable<Record> records = recordRepository.findAll();
        for (Record record : records) {
            log.info("\n" + writer.writeValueAsString(record));
        }
        log.info("\n===========================");
        log.info("\n\n");

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/clean")
    public ResponseEntity<?> cleanScreen() {
        protocolService.onCleanup();

        for (int i = 0; i < 100; i++) {
            log.info("\n");
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/mode/{name}")
    public ResponseEntity<?> switchProtocol(@PathVariable String name) {
        cleanScreen();
        boolean res = protocolService.switchProtocol(name);
        recordRepository.deleteAll();

        return new ResponseEntity<>(res ? HttpStatus.OK : HttpStatus.BAD_REQUEST);
    }

    @PostMapping("/cleandb")
    public ResponseEntity<?> switchProtocol() {
        recordRepository.deleteAll();
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
