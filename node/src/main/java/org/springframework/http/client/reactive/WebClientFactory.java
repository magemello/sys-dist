package org.springframework.http.client.reactive;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.bootstrap.Bootstrap;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.http.client.HttpClient;
import reactor.ipc.netty.http.client.HttpClientOptions;
import reactor.ipc.netty.http.client.HttpClientRequest;
import reactor.ipc.netty.http.client.HttpClientResponse;
import reactor.ipc.netty.options.NettyOptions;

/**
 * Please do not look at this horrible code. It's a complicated piece
 * of reflection shit in order to have netty binding the right local
 * ports so that we can freeze the clients.
 * 
 * Sorry about that.
 * @author bbossola
 *
 */
public class WebClientFactory implements ClientHttpConnector {

    private static int count = 0;
    private final HttpClient httpClient;

    public WebClientFactory(Consumer<? super HttpClientOptions.Builder> clientOptions) {
        this.httpClient = HttpClient.create(clientOptions);
        fixLocalAddress(System.getProperty("server.address"));
    }

    private void fixLocalAddress(String localAddress) {
        if (count++ == 0)
            System.err.println("Forced local address: "+localAddress);

        if (localAddress != null) {
            HttpClientOptions options = (HttpClientOptions) getFieldValue(HttpClient.class, "options", this.httpClient);
            Bootstrap bootstrap = (Bootstrap) getFieldValue(NettyOptions.class, "bootstrapTemplate", options);
            bootstrap.localAddress(new InetSocketAddress(localAddress, 0));
        }
    }

    private Object getFieldValue(Class<?> clazz, String name, Object instance) {
        try {
            Field fieldOptions = clazz.getDeclaredField(name);
            fieldOptions.setAccessible(true);
            return fieldOptions.get(instance);
        }
        catch (Exception ex) {
            throw new RuntimeException("Cannot get field "+name+" value from class "+clazz.getName(), ex);
        }
    }

    @Override
    public Mono<ClientHttpResponse> connect(HttpMethod method, URI uri,
            Function<? super ClientHttpRequest, Mono<Void>> requestCallback) {

        if (!uri.isAbsolute()) {
            return Mono.error(new IllegalArgumentException("URI is not absolute: " + uri));
        }

        return this.httpClient
                .request(adaptHttpMethod(method),
                        uri.toString(),
                        request -> requestCallback.apply(adaptRequest(method, uri, request)))
                .map(this::adaptResponse);
    }

    private io.netty.handler.codec.http.HttpMethod adaptHttpMethod(HttpMethod method) {
        return io.netty.handler.codec.http.HttpMethod.valueOf(method.name());
    }

    private ReactorClientHttpRequest adaptRequest(HttpMethod method, URI uri, HttpClientRequest request) {
        return new ReactorClientHttpRequest(method, uri, request);
    }

    private ClientHttpResponse adaptResponse(HttpClientResponse response) {
        return new ReactorClientHttpResponse(response);
    }
    
    public static WebClient newWebClient() {
        WebClientFactory connector = new WebClientFactory(opt -> {
            opt.disablePool();
        });

        return WebClient.builder().clientConnector(connector).build();
    }
}
