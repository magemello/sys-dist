package org.magemello.sys.node.clients;

import org.magemello.sys.node.domain.VoteRequest;
import org.magemello.sys.node.service.P2PService;
import org.magemello.sys.protocol.raft.Update;
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

@Service
public class CPProtocolClient {

    @Autowired
    private P2PService p2pService;

    @Value("${client.timeout:3}")
    private Integer clientTimeout;

    public Flux<ClientResponse> requestVotes(Integer whoami, Integer term) {
        return Flux.fromIterable(p2pService.getPeers())
                .flatMap(peer -> createWebClientVote(whoami, term, peer), p2pService.getPeers().size())
                .timeout(Duration.ofMillis(clientTimeout))
                .onErrorResume(throwable -> Mono.just(ClientResponse.create(HttpStatus.REQUEST_TIMEOUT).build()))
                .filter(response -> !response.statusCode().isError());
    }

    private Mono<ClientResponse> createWebClientVote(Integer whoami, Integer term, String peer) {
        return WebClient.create()
                .post()
                .uri("http://" + peer + "/cp/voteforme")
                .accept(MediaType.APPLICATION_JSON)
                .syncBody(new VoteRequest(whoami, term))
                .exchange()
                .onErrorResume(throwable -> Mono.just(ClientResponse.create(HttpStatus.BAD_GATEWAY).build()));
    }


    // FIXME - the API returns success as soon as it receives as many responses as the requested quorum
    public boolean sendBeat(Update update, Integer quorum) {
        Flux.fromIterable(p2pService.getPeers())
                .flatMap(peer -> createWebClientBeat(update, peer), p2pService.getPeers().size())
                .timeout(Duration.ofMillis(clientTimeout))
                .onErrorResume(throwable -> Mono.just(ClientResponse.create(HttpStatus.REQUEST_TIMEOUT).build()))
                .filter(response -> !response.statusCode().isError()).count().block();
        
        return false;
    }

    private Mono<ClientResponse> createWebClientBeat(Update update, String peer) {
        return WebClient.create()
                .post()
                .uri("http://" + peer + "/cp/update")
                .accept(MediaType.APPLICATION_JSON)
                .syncBody(update)
                .exchange()
                .onErrorResume(throwable -> Mono.just(ClientResponse.create(HttpStatus.BAD_GATEWAY).build()));
    }
}
