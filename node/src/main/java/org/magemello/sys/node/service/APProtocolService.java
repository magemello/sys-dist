package org.magemello.sys.node.service;

import org.magemello.sys.node.clients.ACProtocolClient;
import org.magemello.sys.node.clients.APProtocolClient;
import org.magemello.sys.node.domain.Record;
import org.magemello.sys.node.domain.Response;
import org.magemello.sys.node.repository.RecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Service("AP")
public class APProtocolService implements ProtocolService {

    private static final Logger log = LoggerFactory.getLogger(APProtocolService.class);

    @Autowired
    RecordRepository recordRepository;

    @Autowired
    private APProtocolClient apProtocolClient;

    private Map<String, Record> writeAheadLog = new LinkedHashMap<>();

    private Set<String> keys = new HashSet<>();

    @Override
    public Record get(String key) {
        log.info("AP Service - get for {} ", key);

        return recordRepository.findByKey(key);
    }

    @Override
    public Mono<Response> set(String key, String value) throws Exception {
        recordRepository.save(new Record(key, value));
        return Mono.empty();
    }

    @Override
    public String protocolName() {
        return "AP";
    }


    public boolean propose(Record record) {
        log.info("AP Service - Propose for {} ", record);

        if (!keys.contains(record.getKey())) {
            keys.add(record.getKey());
            writeAheadLog.put(record.get_ID(), record);
            return true;
        }
        return false;
    }

    public Record commit(String id) {
        log.info("AP Service - Commit id {} ", id);

        Record record = writeAheadLog.get(id);

        if (record != null) {
            recordRepository.save(record);
            writeAheadLog.remove(id);
            keys.remove(record.getKey());
        }
        return record;
    }

    public Record rollback(String id) {
        log.info("AP Service - Rollback id {} ", id);

        Record record = writeAheadLog.get(id);

        if (record != null) {
            writeAheadLog.remove(id);
            keys.remove(record.getKey());
        }
        return record;
    }

    public Record repair(Record record) {
        log.info("AP Service - Repair id {} ", record);

        return recordRepository.save(record);
    }

    public void clearWriteHaeadLog() {
        writeAheadLog.clear();
        keys.clear();
    }
}
