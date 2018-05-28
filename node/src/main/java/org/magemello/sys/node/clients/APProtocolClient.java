package org.magemello.sys.node.clients;

import org.magemello.sys.node.domain.Record;
import org.magemello.sys.node.domain.Transaction;
import org.magemello.sys.node.service.P2PService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class APProtocolClient {

    @Value("${client.timeout:3}")
    private Integer clientTimeout;

    @Autowired
    private P2PService p2pService;


    public Mono<List<ClientResponse>> propose(Transaction transaction) {
        return Flux.fromIterable(p2pService.getPeers())
                .flatMap(peer -> createWebClientPropose(transaction, peer), p2pService.getPeers().size())
                .timeout(Duration.ofSeconds(clientTimeout))
                .onErrorResume(throwable -> Mono.just(ClientResponse.create(HttpStatus.REQUEST_TIMEOUT).build())).collectList();
    }

    public Mono<Long> commit(String id) {
        return Flux.fromIterable(p2pService.getPeers())
                .flatMap(peer -> createWebClientCommit(id, peer), p2pService.getPeers().size())
                .timeout(Duration.ofSeconds(clientTimeout))
                .filter(response -> !response.statusCode().isError())
                .count();
    }

    public Mono<Boolean> rollback(String id, List<ClientResponse> clientResponses) {

        List<String> peers = getNotFailingPeers(clientResponses);

        return Flux.fromIterable(peers)
                .flatMap(peer -> createWebClientRollBack(id, peer), p2pService.getPeers().size())
                .timeout(Duration.ofSeconds(clientTimeout))
                .onErrorResume(throwable -> Mono.just(ClientResponse.create(HttpStatus.REQUEST_TIMEOUT).build()))
                .all(response -> !response.statusCode().isError());
    }

    public Mono<Boolean> repair(List<ClientResponse> clientResponses, Record record) {

        List<String> peers = getDisaccordingPeers(clientResponses, record);

        return Flux.fromIterable(peers)
                .flatMap(peer -> createWebClientRepair(record, peer), p2pService.getPeers().size())
                .timeout(Duration.ofSeconds(clientTimeout))
                .all(response -> !response.statusCode().isError());
    }

    public Flux<ClientResponse> read(String key) {
        return Flux.fromIterable(p2pService.getPeers())
                .flatMap(peer -> createWebClientRead(key, peer), p2pService.getPeers().size());
    }

    private Mono<ClientResponse> createWebClientPropose(Transaction transaction, String peer) {
        return WebClient.create()
                .post()
                .uri("http://" + peer + "/ap/propose")
                .syncBody(transaction)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .onErrorResume(throwable -> Mono.just(ClientResponse.create(HttpStatus.BAD_GATEWAY).build()));
    }

    private Mono<ClientResponse> createWebClientCommit(String id, String peer) {
        return WebClient.create()
                .post()
                .uri("http://" + peer + "/ap/commit/" + id)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .onErrorResume(throwable -> Mono.just(ClientResponse.create(HttpStatus.BAD_GATEWAY).build()));
    }

    private Mono<ClientResponse> createWebClientRollBack(String id, String peer) {
        return WebClient.create()
                .post()
                .uri("http://" + peer + "/ap/rollback/" + id)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .onErrorResume(throwable -> Mono.just(ClientResponse.create(HttpStatus.BAD_GATEWAY).build()));
    }

    private Mono<ClientResponse> createWebClientRepair(Record record, String peer) {
        return WebClient.create()
                .post()
                .uri("http://" + peer + "/ap/repair")
                .syncBody(record)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .onErrorResume(throwable -> Mono.just(ClientResponse.create(HttpStatus.BAD_GATEWAY).build()));
    }

    private Mono<ClientResponse> createWebClientRead(String key, String peer) {
        return WebClient.create()
                .get()
                .uri("http://" + peer + "/ap/read/" + key)
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

    private List<String> getDisaccordingPeers(List<ClientResponse> clientResponses, Record record) {
        return clientResponses.stream().filter(clientResponse -> clientResponse.bodyToMono(Record.class).block().equals(record))
                .map(clientResponse -> clientResponse.headers().header("x-sys-ip").stream().findFirst().get().toString())
                .collect(Collectors.toList());
    }
}
