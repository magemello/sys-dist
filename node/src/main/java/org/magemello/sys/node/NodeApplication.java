package org.magemello.sys.node;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.magemello.sys.node.repository.RecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    @Value("${server.address}")
    private String serverAddress;

    @Autowired
    RecordRepository recordRepository;

    public static void main(String[] args) {
        new SpringApplicationBuilder(NodeApplication.class)
            .logStartupInfo(false)
            .run(args);
    }

    @Bean
    public ApplicationListener<ContextRefreshedEvent> startupLoggingListener() {
        return event -> {
            if (serverDelay != 0)
                log.info("\n...and this server is slow! ({} msec)\n", serverDelay);
        };
    }

}

@Configuration
class WebMvcConfig implements WebMvcConfigurer {

    @Value("${server.delay:0}")
    private Long serverDelay;

    @Value("${server.port}")
    private String serverPort;

    @Value("${server.address}")
    private String serverAddress;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {

            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
                    throws Exception {

                String pathInfo = request.getRequestURI();
                if (!pathInfo.startsWith("/demo"))
                    Thread.sleep(serverDelay);

                if (response.getHeader("x-sys-ip") == null) {
                    response.addHeader("x-sys-ip", serverAddress + ":" + serverPort);
                }
                return true;
            }
        });
    }
}
