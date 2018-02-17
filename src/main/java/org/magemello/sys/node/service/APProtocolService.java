package org.magemello.sys.node.service;

import org.magemello.sys.node.domain.Record;
import org.magemello.sys.node.repository.RecordRepository;
import org.springframework.beans.factory.annotation.Autowired;

public class APProtocolService implements ProtocolService {

    @Autowired
    RecordRepository recordRepository;

    @Override
    public Record get(String key) {
        return recordRepository.findByKey(key);
    }

    @Override
    public void set(String key, String value) throws Exception {
        recordRepository.save(new Record(key, value));
    }
}
