package org.magemello.sys.node.service;

import org.magemello.sys.node.domain.Record;
import org.magemello.sys.node.repository.RecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;

@Service("AC")
public class ACProtocolService implements ProtocolService {

    private static final Logger log = LoggerFactory.getLogger(ACProtocolService.class);

    @Autowired
    RecordRepository recordRepository;

    LinkedHashMap<Long, Record> writeAheadLog = new LinkedHashMap<>();

    @Override
    public Record get(String key) {
        return recordRepository.findByKey(key);
    }

    @Override
    public void set(String key, String value) throws Exception {
        recordRepository.save(new Record(key, value));
    }

    @Override
    public String protocolName() {
        return "AC";
    }

    public boolean vote(Record record) {
        if (isANewVote(record)) {
            writeAheadLog.put(record.get_ID(), record);
            return true;
        }
        return false;
    }

    public Record commit(Long id) {
        Record record = writeAheadLog.get(id);

        if (record != null) {
            recordRepository.save(record);
            writeAheadLog.remove(id);
        }
        return record;
    }

    public Record rollback(Long id) {
        Record record = writeAheadLog.get(id);

        if (record != null) {
            recordRepository.delete(record);
            writeAheadLog.remove(id);
        }
        return record;
    }

    public void clearWriteHaeadLog(){
        writeAheadLog.clear();
    }

    private boolean isANewVote(Record record) {
        return writeAheadLog.get(record.get_ID()).getKey() != record.getKey();
    }
}
