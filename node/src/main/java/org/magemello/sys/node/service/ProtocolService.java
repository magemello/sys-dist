package org.magemello.sys.node.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@SuppressWarnings("rawtypes")
public interface ProtocolService {

    Mono<ResponseEntity> get(String key);

    Mono<ResponseEntity> set(String key, String value) throws Exception;

    void reset();

    String protocolName();
    
    void start();
    
    void stop();
}
