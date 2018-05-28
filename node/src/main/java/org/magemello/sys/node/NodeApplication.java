package org.magemello.sys.node;

import org.magemello.sys.node.repository.RecordRepository;
import org.magemello.sys.node.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@SpringBootApplication
@EnableWebFlux
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

@RefreshScope
@Configuration
class WebMvcConfig implements WebMvcConfigurer {

    @Value("${server.delay:0}")
    private Long clientTimeout;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {


            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
                    throws Exception {
                Thread.sleep(clientTimeout);
                return true;
            }
        });
    }
}
