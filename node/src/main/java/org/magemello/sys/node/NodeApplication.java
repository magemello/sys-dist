package org.magemello.sys.node;

import org.magemello.sys.node.repository.RecordRepository;
import org.magemello.sys.node.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;


@SpringBootApplication
public class NodeApplication {

    @Autowired
    RecordRepository recordRepository;

    @Autowired
    ProtocolFactory protocolFactory;

    public static void main(String[] args) {
        SpringApplication.run(NodeApplication.class, args);
    }

    @Bean
    @Primary
    @RefreshScope
    public ProtocolService provideProtocol() {
        return protocolFactory.getProtocol();
    }
}
