package org.magemello.sys.node.service;

import org.magemello.sys.node.domain.Record;
import org.springframework.stereotype.Service;

@Service
public interface ProtocolStorageService {

    Record get(String key);

    void set(String key,Record record);
}
