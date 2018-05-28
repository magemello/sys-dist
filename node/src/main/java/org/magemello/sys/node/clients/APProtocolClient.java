package org.magemello.sys.node.clients;

import org.magemello.sys.node.domain.Record;
import org.magemello.sys.node.domain.Transaction;
import org.magemello.sys.node.service.P2PService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class APProtocolClient {

    @Autowired
    private P2PService p2pService;

    public Mono<Long> propose(Transaction transaction) {
        return Flux.fromIterable(p2pService.getPeers())
                .flatMap(peer -> createWebClientPropose(transaction, peer), p2pService.getPeers().size())
                .timeout(Duration.ofSeconds(10))
                .filter(response -> !response.statusCode().isError())
                .count();
    }

    public Mono<Long> commit(String id) {
        return Flux.fromIterable(p2pService.getPeers())
                .flatMap(peer -> createWebClientCommit(id, peer), p2pService.getPeers().size())
                .timeout(Duration.ofSeconds(10))
                .filter(response -> !response.statusCode().isError())
                .count();
    }

    public Mono<Boolean> rollback(String id) {
        return Flux.fromIterable(p2pService.getPeers())
                .flatMap(peer -> createWebClientRollBack(id, peer), p2pService.getPeers().size())
                .timeout(Duration.ofSeconds(10))
                .all(response -> !response.statusCode().isError());
    }

    public Mono<Boolean> repair(Record record) {
        return Flux.fromIterable(p2pService.getPeers())
                .flatMap(peer -> createWebClientRepair(record, peer), p2pService.getPeers().size())
                .timeout(Duration.ofSeconds(10))
                .all(response -> !response.statusCode().isError());
    }

    public Flux<Record> read(String key) {
        return Flux.fromIterable(p2pService.getPeers())
                .flatMap(peer -> createWebClientRead(key, peer), p2pService.getPeers().size());
    }

    private Mono<ClientResponse> createWebClientPropose(Transaction transaction, String peer) {
        return WebClient.create()
                .post()
                .uri(peer + "ap/propose")
                .syncBody(transaction)
                .accept(MediaType.APPLICATION_JSON)
                .exchange();
    }

    private Mono<ClientResponse> createWebClientCommit(String id, String peer) {
        return WebClient.create()
                .post()
                .uri(peer + "ap/commit/" + id)
                .accept(MediaType.APPLICATION_JSON)
                .exchange();
    }

    private Mono<ClientResponse> createWebClientRollBack(String id, String peer) {
        return WebClient.create()
                .post()
                .uri(peer + "ap/rollback/" + id)
                .accept(MediaType.APPLICATION_JSON)
                .exchange();
    }

    private Mono<ClientResponse> createWebClientRepair(Record record, String peer) {
        return WebClient.create()
                .post()
                .uri(peer + "ap/repair")
                .syncBody(record)
                .accept(MediaType.APPLICATION_JSON)
                .exchange();
    }

    private Mono<Record> createWebClientRead(String key, String peer) {
        return WebClient.create()
                .get()
                .uri(peer + "ap/read/" + key)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Record.class)
                .onErrorResume(throwable -> Mono.empty());
    }
}
