package org.magemello.sys.node.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public interface ProtocolService {

    Mono<ResponseEntity> get(String key);

    Mono<ResponseEntity> set(String key, String value) throws Exception;

    String protocolName();
}
