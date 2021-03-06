package org.magemello.sys.node.protocols.ac.clients;

import org.magemello.sys.node.protocols.ac.domain.Transaction;
import org.magemello.sys.node.service.P2PService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import static org.springframework.http.client.reactive.WebClientFactory.newWebClient;

@Service
public class ACProtocolClient {

    @Value("${client.timeout:3}")
    private Integer clientTimeout;

    @Autowired
    private P2PService p2pService;

    public Mono<List<ClientResponse>> propose(Transaction transaction) {
        return Flux.fromIterable(p2pService.getPeers())
                .flatMap(peer -> createWebClientPropose(transaction, peer), p2pService.getPeers().size())
                .timeout(Duration.ofMillis(clientTimeout))
                .onErrorResume(throwable -> Mono.just(ClientResponse.create(HttpStatus.REQUEST_TIMEOUT).build())).collectList();
    }

    public Mono<Boolean> commit(String id) {
        return Flux.fromIterable(p2pService.getPeers())
                .flatMap(peer -> createWebClientCommit(id, peer), p2pService.getPeers().size())
                .timeout(Duration.ofMillis(clientTimeout))
                .onErrorResume(throwable -> Mono.just(ClientResponse.create(HttpStatus.REQUEST_TIMEOUT).build()))
                .all(response -> !response.statusCode().isError());
    }

    public Mono<Boolean> rollback(String id, List<ClientResponse> clientResponses) {

        List<String> peers = getNotFailingPeers(clientResponses);

        return Flux.fromIterable(peers)
                .flatMap(peer -> createWebClientRollBack(id, peer), p2pService.getPeers().size())
                .timeout(Duration.ofMillis(clientTimeout))
                .onErrorResume(throwable -> Mono.just(ClientResponse.create(HttpStatus.REQUEST_TIMEOUT).build()))
                .all(response -> !response.statusCode().isError());
    }

    private Mono<ClientResponse> createWebClientPropose(Transaction transaction, String peer) {
        return newWebClient()
                .post()
                .uri("http://" + peer + "/ac/propose")
                .syncBody(transaction)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .onErrorResume(throwable -> Mono.just(ClientResponse.create(HttpStatus.BAD_GATEWAY).build()));

    }

    private Mono<ClientResponse> createWebClientCommit(String id, String peer) {
        return newWebClient()
                .post()
                .uri("http://" + peer + "/ac/commit/" + id)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .onErrorResume(throwable -> Mono.just(ClientResponse.create(HttpStatus.BAD_GATEWAY).build()));
    }

    private Mono<ClientResponse> createWebClientRollBack(String id, String peer) {
        return newWebClient()
                .post()
                .uri("http://" + peer + "/ac/rollback/" + id)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .onErrorResume(throwable -> Mono.just(ClientResponse.create(HttpStatus.BAD_GATEWAY).build()));
    }

    private List<String> getNotFailingPeers(List<ClientResponse> clientResponses) {
        return clientResponses.stream()
                .filter(clientResponse -> !clientResponse.statusCode().isError())
                .map(clientResponse -> clientResponse.headers().header("x-sys-ip").stream().findFirst().get().toString())
                .collect(Collectors.toList());
    }
}
