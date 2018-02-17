package org.magemello.sys.node.service;

import java.io.Console;

import javax.annotation.PostConstruct;

import org.aeonbits.owner.event.ReloadEvent;
import org.aeonbits.owner.event.ReloadListener;
import org.magemello.sys.node.Configuration;
import org.magemello.sys.node.repository.RecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConfigurationListener {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationListener.class);
    
    @Autowired
    ProtocolStorageFactory storageFactory;

    @Autowired
    RecordRepository recordRepository;

    @Autowired
    ACProtocolService protocolAC;
    @Autowired
    APProtocolService protocolAP;
    @Autowired
    CPProtocolService protocolCP;

    @Autowired
    Configuration configuration;

    public ConfigurationListener() {
    }
    
    @PostConstruct
    public void init() {
        configuration.addReloadListener(new ReloadListener() {
            @Override
            public void reloadPerformed(ReloadEvent event) {
                reload();
            }});

        reload();
    }
    
    private void reload() {
        System.out.printf("Protocol selected: %s\n", configuration.protocol());

        recordRepository.deleteAll();
        storageFactory.setProtocolStorage(getProtocolStorage());
    }

    private ProtocolStorage getProtocolStorage() {
        switch(configuration.protocol()) {
            case "AC":  return protocolAC;
            case "AP":  return protocolAP;
            case "CP":  return protocolCP;
            
            default:
                log.error("Invalid protocol: "+configuration.protocol());
                return protocolAC;
        }
    }
}
