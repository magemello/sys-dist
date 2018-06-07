package org.magemello.sys.node.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import javax.annotation.PostConstruct;


import org.magemello.sys.node.protocols.ac.service.ACProtocolService;
import org.magemello.sys.node.protocols.ap.service.APProtocolService;
import org.magemello.sys.node.protocols.cp.service.CPProtocolService;
import org.magemello.sys.node.repository.RecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

@Primary
@Service
@SuppressWarnings("rawtypes")
public class ProtocolServiceProxy implements ProtocolService {

    private static final Logger log = LoggerFactory.getLogger(ProtocolServiceProxy.class);

    public static final String CP = "CP";
    public static final String AP = "AP";
    public static final String AC = "AC";

    @Autowired
    ACProtocolService protocolAC;

    @Autowired
    APProtocolService protocolAP;

    @Autowired
    CPProtocolService protocolCP;

    @Autowired
    RecordRepository recordRepository;

    ProtocolService current;

    @PostConstruct
    public void init() {
        switchProtocol(loadCurrentProtocol(AC));
    }

    public boolean switchProtocol(String proto) {
        ProtocolService requested = selectProtocolService(proto);
        if (requested == null) {
            log.error("Invalid protocol selected: \"" + proto + "\"");
            return false;
        }
        if (current != null) {
            current.stop();
        }

        recordRepository.deleteAll();

        current = requested;
        current.start();

        storeCurrentProtocol(proto);

        return true;
    }

    private ProtocolService selectProtocolService(String name) {
        switch (name) {
            case AC:
                return protocolAC;
            case AP:
                return protocolAP;
            case CP:
                return protocolCP;
            default:
                return null;
        }

    }

    @Override
    public Mono<ResponseEntity> get(String key) {
        return current.get(key);
    }

    @Override
    public Mono<ResponseEntity> set(String key, String value) throws Exception {
        return current.set(key, value);
    }

    @Override
    public void onCleanup() {
        current.onCleanup();
    }

    @Override
    public String protocolName() {
        return current.protocolName();
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    private static final Path CONFIG_FILE = Paths.get(System.getProperty("user.home"), ".sysdist");

    private String loadCurrentProtocol(String defval) {
        try {
            return new String(Files.readAllBytes(CONFIG_FILE));
        } catch (Exception ignore) {
            ignore.printStackTrace();
            return defval;
        }
    }

    private void storeCurrentProtocol(String protocol) {
        try {
            Files.write(CONFIG_FILE, protocol.getBytes(), StandardOpenOption.CREATE);
        } catch (IOException ignore) {
            ignore.printStackTrace();
        }
    }

}
