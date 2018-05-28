package org.magemello.sys.node;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.magemello.sys.node.repository.RecordRepository;
import org.magemello.sys.node.service.ProtocolFactory;
import org.magemello.sys.node.service.ProtocolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@SpringBootApplication
@EnableWebFlux
public class NodeApplication {

    private static final Logger log = LoggerFactory.getLogger(NodeApplication.class);

    @Value("${server.delay:0}")
    private Long serverDelay;

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
    
    @Bean
    public ApplicationListener<ContextRefreshedEvent> startupLoggingListener() {
        return new ApplicationListener<ContextRefreshedEvent>() {   
            public void onApplicationEvent(ContextRefreshedEvent event) {
                if (serverDelay != 0)
                    log.info("...and this server is slow! ({} msec)", serverDelay);
            }       
        };
    }
    
}

@RefreshScope
@Configuration
class WebMvcConfig implements WebMvcConfigurer {

    @Value("${server.delay:0}")
    private Long serverDelay;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {


            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
                    throws Exception {
                Thread.sleep(serverDelay);
                return true;
            }
        });
    }
}
