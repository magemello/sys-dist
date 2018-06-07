package org.magemello.sys.node.protocols.cp.service;

import org.magemello.sys.node.domain.RecordTerm;
import org.magemello.sys.node.domain.VoteRequest;
import org.magemello.sys.node.protocols.cp.clients.CPProtocolClient;
import org.magemello.sys.node.protocols.cp.raft.Update;
import org.magemello.sys.node.repository.RecordTermRepository;
import org.magemello.sys.node.service.ProtocolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

@Service("CP")
@SuppressWarnings("rawtypes")
public class CPProtocolService implements ProtocolService {

    private static final Logger log = LoggerFactory.getLogger(CPProtocolService.class);

    @Value("${server.port}")
    private String serverPort;

    @Value("${server.address}")
    private String serverAddress;

    @Autowired
    private RecordTermRepository recordTermRepository;

    @Autowired
    private CPProtocolClient client;

    @Autowired
    private RaftService raft;

    private String leaderAddress;

    @Override
    public Mono<ResponseEntity> get(String key) {

        RecordTerm record = recordTermRepository.findByKey(key);
        if (record == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
        } else {
            return Mono.just(ResponseEntity.status(HttpStatus.OK).body("QUORUM " + record.toString()));
        }
    }

    @Override
    public Mono<ResponseEntity> set(String key, String value) throws Exception {
        if (raft.amITheLeader()) {
            log.info("- I'm the leader scheduling data for the next beat");

            if (raft.setData(key, value)) {
                log.info("- Data scheduled");

                return Mono.just(ResponseEntity.status(HttpStatus.OK).build());
            } else {
                log.info("- Buffer full data not scheduled");

                return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build());
            }
        }

        if (raft.amIAFollower()) {
            log.info("- Forwarding write request of {} to leader {} for value {}", key, leaderAddress, value);

            ClientResponse clientResponse = client.forwardDataToLeader(key, value, leaderAddress).block();
            log.info("- Status write request {} ", clientResponse.statusCode());

            return Mono.just(ResponseEntity.status(clientResponse.statusCode()).build());
        }

        log.info("- No leader elected yet");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("No leader at the moment!"));
    }

    @Override
    public String protocolName() {
        return "CP";
    }

    @Override
    public void onCleanup() {
    }

    @Override
    public void start() {
        log.info("CP mode (majority quorum, raft)");
        raft.start();
    }

    @Override
    public void stop() {
        raft.stop();
    }

    public boolean vote(VoteRequest vote) {
        return raft.handleVoteRequest(vote);
    }

    public boolean beat(Update update) {
        this.leaderAddress = serverAddress + update.from.toString();
        return raft.handleBeat(update);
    }
}
