package org.magemello.sys.node.service;

import org.magemello.sys.node.repository.RecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
@RefreshScope
public class ProtocolFactory {

    private static final Logger log = LoggerFactory.getLogger(ProtocolFactory.class);

    @Value("${protocol:AP}")
    private String protocol;

    @Autowired
    ACProtocolService protocolAC;

    @Autowired
    APProtocolService protocolAP;

    @Autowired
    CPProtocolService protocolCP;

    @Autowired
    RecordRepository recordRepository;

    @PostConstruct
    public void init() {
        log.info("Cleaning storage");
        recordRepository.deleteAll();
    }

    public ProtocolService getProtocol() {
        log.info("Switching to {} protocol", protocol);

        switch (protocol) {
            case "AC":
                return protocolAC;
            case "AP":
                return protocolAP;
            case "CP":
                return protocolCP;

            default:
                log.error("Switching attempt failed, {} is not a supported protocol - returning default protocol AC ", protocol);
                return protocolAC;
        }
    }

}
