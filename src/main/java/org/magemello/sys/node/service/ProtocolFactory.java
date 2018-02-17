package org.magemello.sys.node.service;

import org.springframework.stereotype.Service;

@Service
public class ProtocolFactory {
    
    public ProtocolStorageService getProtocolStorage() {
        return new ACProtocolStorageService();
    }
}
