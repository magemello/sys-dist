package org.magemello.sys.node.protocols.cp.clients;

import org.magemello.sys.node.domain.RecordTerm;
import org.magemello.sys.node.domain.VoteRequest;
import org.magemello.sys.node.protocols.cp.domain.Update;
import org.magemello.sys.node.service.P2PService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class CPProtocolClient {

    @Autowired
    private P2PService p2pService;

    @Value("${client.timeout:3}")
    private Integer clientTimeout;

    @Value("${server.port}")
    private String serverPort;

    public Mono<ClientResponse> forwardDataToLeader(String key, String value, int port) {
        return WebClient.create()
                .post()
                .uri("http://127.0.0." + (port - 3000) + ":" + port + "/storage/" + key + "/" + value)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .onErrorResume(throwable -> Mono.just(ClientResponse.create(HttpStatus.BAD_GATEWAY).build()));
    }

    public Mono<Long> sendBeat(Update update, Integer quorum) {
        return new Mono<Long>() {

            private CoreSubscriber<? super Long> context;
            private AtomicLong responses = new AtomicLong(0);

            @Override
            public void subscribe(CoreSubscriber<? super Long> actual) {
                this.context = actual;
                sendBeat(update)
                        .map(this::manageUpdateQuorum)
                        .count()
                        .subscribe(quorum -> {
                            actual.onNext(quorum);
                            actual.onComplete();
                        });
            }

            private ClientResponse manageUpdateQuorum(ClientResponse clientResponse) {
                if (responses.incrementAndGet() >= quorum) {
                    context.onNext(responses.incrementAndGet());
                    context.onComplete();
                }

                return clientResponse;
            }
        };
    }

    private Flux<ClientResponse> sendBeat(Update update) {
        return Flux.fromIterable(p2pService.getPeers())
                .flatMap(peer -> createWebClientSendBeat(update, peer), p2pService.getPeers().size())
                .timeout(Duration.ofMillis(clientTimeout))
                .onErrorResume(throwable -> Mono.just(ClientResponse.create(HttpStatus.REQUEST_TIMEOUT).build()))
                .filter(response -> !response.statusCode().isError());
    }

    private Mono<ClientResponse> createWebClientSendBeat(Update update, String peer) {
        return WebClient.create()
                .post()
                .uri("http://" + peer + "/cp/update")
                .accept(MediaType.APPLICATION_JSON)
                .syncBody(update)
                .exchange()
                .onErrorResume(throwable -> Mono.just(ClientResponse.create(HttpStatus.BAD_GATEWAY).build()));
    }

    public Flux<RecordTerm> history(Integer term, Integer tick, String leaderAddress) {
        return WebClient.create()
                .get()
                .uri("http://" + leaderAddress + "/cp/history/" + term.toString() + "/" + tick.toString())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToFlux(RecordTerm.class);
    }

    public Mono<Long> requestVotes(Integer term, int quorum) {
        return new Mono<Long>() {

            private CoreSubscriber<? super Long> context;
            private AtomicLong responses = new AtomicLong(0);

            @Override
            public void subscribe(CoreSubscriber<? super Long> actual) {
                this.context = actual;

                requestVotes(term)
                        .map(this::manageRequestVoteQuorum)
                        .count()
                        .subscribe(quorum -> {
                            actual.onNext(quorum);
                            actual.onComplete();
                        });
            }

            private ClientResponse manageRequestVoteQuorum(ClientResponse clientResponse) {
                if (responses.incrementAndGet() >= quorum) {
                    context.onNext(responses.incrementAndGet());
                    context.onComplete();
                }

                return clientResponse;
            }
        };
    }

    private Flux<ClientResponse> requestVotes(Integer term) {
        return Flux.fromIterable(p2pService.getPeers())
                .flatMap(peer -> createWebClientVote(term, peer), p2pService.getPeers().size())
                .timeout(Duration.ofMillis(clientTimeout))
                .onErrorResume(throwable -> Mono.just(ClientResponse.create(HttpStatus.REQUEST_TIMEOUT).build()))
                .filter(response -> !response.statusCode().isError());
    }

    private Mono<ClientResponse> createWebClientVote(Integer term, String peer) {
        return WebClient.create()
                .post()
                .uri("http://" + peer + "/cp/voteforme")
                .accept(MediaType.APPLICATION_JSON)
                .syncBody(new VoteRequest(Integer.parseInt(serverPort), term))
                .exchange()
                .onErrorResume(throwable -> Mono.just(ClientResponse.create(HttpStatus.BAD_GATEWAY).build()));
    }
}
