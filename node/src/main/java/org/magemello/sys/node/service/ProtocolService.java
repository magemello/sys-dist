package org.magemello.sys.node.service;

import org.magemello.sys.node.domain.Record;
import org.magemello.sys.node.domain.Response;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public interface ProtocolService {

    Record get(String key);

    Mono<Response> set(String key, String value) throws Exception;

    String protocolName();
}
