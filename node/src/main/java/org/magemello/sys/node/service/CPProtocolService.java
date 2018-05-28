package org.magemello.sys.node.service;

import org.magemello.sys.node.domain.Record;
import org.magemello.sys.node.repository.RecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service("CP")
public class CPProtocolService implements ProtocolService {

    @Autowired
    RecordRepository recordRepository;

    @Override
    public Mono<ResponseEntity> get(String key) {
        recordRepository.findByKey(key);
        return Mono.empty();
    }

    @Override
    public Mono<ResponseEntity> set(String key, String value) throws Exception {
        recordRepository.save(new Record(key, value));
        return Mono.empty();
    }

    @Override
    public String protocolName() {
        return "CP";
    }

    @Override
    public void clean(){

    }
}
