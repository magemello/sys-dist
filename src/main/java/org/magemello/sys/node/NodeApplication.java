package org.magemello.sys.node;

import org.magemello.sys.node.service.ProtocolStorageFactoryService;
import org.magemello.sys.node.service.ProtocolStorageService;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;

@SpringBootApplication
public class NodeApplication {

	public static void main(String[] args) {
		SpringApplication.run(NodeApplication.class, args);
	}

	@Bean
	@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public ProtocolStorageService personPrototype() {
		ProtocolStorageFactoryService protocolStorageFactoryService =  new ProtocolStorageFactoryService();
		protocolStorageFactoryService
	}
}