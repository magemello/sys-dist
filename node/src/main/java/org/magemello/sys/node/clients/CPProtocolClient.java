package org.magemello.sys.node.clients;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import org.magemello.sys.node.domain.VoteRequest;
import org.magemello.sys.node.service.P2PService;
import org.magemello.sys.protocol.raft.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@Service
public class CPProtocolClient {

    private static final Logger log = LoggerFactory.getLogger(CPProtocolClient.class);

    @Autowired
    private P2PService p2pService;

    @Value("${client.timeout:3}")
    private Integer clientTimeout;

    public Mono<Long> sendBeat(Update update, Integer quorum) {
        return new Mono<Long>() {

            private CoreSubscriber<? super Long> actual;

            AtomicLong responseQuorum = new AtomicLong(0);

            @Override
            public void subscribe(CoreSubscriber<? super Long> actual) {
                log.info("Sending vote for request to peers");

                this.actual = actual;
                sendBeat(update)
                        .map(this::manageUpdateQuorum)
                        .count()
                        .subscribe(quorum -> {
                            actual.onNext(quorum);
                            actual.onComplete();
                        });
            }

            private ClientResponse manageUpdateQuorum(ClientResponse clientResponse) {
                Long currentQuorum = responseQuorum.incrementAndGet();
                if (currentQuorum >= quorum) {
                    actual.onNext(currentQuorum);
                    actual.onComplete();
                }

                return clientResponse;
            }
        };
    }

    private Flux<ClientResponse> sendBeat(Update update) {
        return Flux.fromIterable(p2pService.getPeers())
                .flatMap(peer -> createWebClientBeat(update, peer), p2pService.getPeers().size())
                .timeout(Duration.ofMillis(clientTimeout))
                .onErrorResume(throwable -> Mono.just(ClientResponse.create(HttpStatus.REQUEST_TIMEOUT).build()))
                .filter(response -> !response.statusCode().isError());
    }


    public Mono<Long> requestVotes(Integer whoami, Integer term, int quorum) {
        return new Mono<Long>() {

            private CoreSubscriber<? super Long> actual;

            AtomicLong voteQuorum = new AtomicLong(0);

            @Override
            public void subscribe(CoreSubscriber<? super Long> actual) {
                log.info("Sending vote request to peers");

                this.actual = actual;

                requestVotes(whoami, term)
                        .map(this::manageRequestVoteQuorum)
                        .count()
                        .subscribe(quorum -> {
                            actual.onNext(quorum);
                            actual.onComplete();
                        });
            }

            private ClientResponse manageRequestVoteQuorum(ClientResponse clientResponse) {
                Long currentQuorum = voteQuorum.incrementAndGet();

                if (currentQuorum >= quorum) {
                    actual.onNext(currentQuorum);
                    actual.onComplete();
                }

                return clientResponse;
            }
        };
    }

    
    private Flux<ClientResponse> requestVotes(Integer whoami, Integer term) {
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
