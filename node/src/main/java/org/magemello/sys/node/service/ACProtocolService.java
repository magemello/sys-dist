package org.magemello.sys.node.service;

import org.magemello.sys.node.domain.Record;
import org.magemello.sys.node.repository.RecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service("AC")
public class ACProtocolService implements ProtocolService {

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

    @Override
    public String test() {
        return "AC";
    }
}
