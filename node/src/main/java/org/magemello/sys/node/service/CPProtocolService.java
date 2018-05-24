package org.magemello.sys.node.service;

import org.magemello.sys.node.domain.Record;
import org.magemello.sys.node.domain.Response;
import org.magemello.sys.node.repository.RecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service("CP")
public class CPProtocolService implements ProtocolService {

    @Autowired
    RecordRepository recordRepository;

    @Override
    public Record get(String key) {
        return recordRepository.findByKey(key);
    }

    @Override
    public Mono<Response> set(String key, String value) throws Exception {
        recordRepository.save(new Record(key, value));
        return Mono.empty();
    }

    @Override
    public String protocolName() {
        return "CP";
    }
}