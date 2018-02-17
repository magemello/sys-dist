package org.magemello.sys.node;

import org.aeonbits.owner.ConfigFactory;
import org.magemello.sys.node.service.ProtocolStorageFactory;
import org.magemello.sys.node.service.ProtocolStorage;
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
	public ProtocolStorage provideProtocol(ProtocolStorageFactory factory) {
		return factory.getProtocolStorage();
	}

	@Bean
    @Scope("singleton")
    public Configuration provideConfiguration() {
//        return ConfigFactory.create(Configuration.class, System.getenv(), System.getProperties());
        return ConfigFactory.create(Configuration.class);
    }
}
