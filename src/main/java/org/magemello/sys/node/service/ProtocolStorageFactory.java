package org.magemello.sys.node.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProtocolStorageFactory {

    private static final Logger log = LoggerFactory.getLogger(ProtocolStorageFactory.class);
    
    @Autowired
    ProtocolStorage storage;
    
    public ProtocolStorage getProtocolStorage() {
        return storage;
    }

    public void setProtocolStorage(ProtocolStorage protocolStorage) {
        this.storage = protocolStorage;
    }
}
