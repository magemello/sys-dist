package org.magemello.sys.node.service;

import org.magemello.sys.node.clients.CPProtocolClient;
import org.magemello.sys.node.domain.VoteRequest;
import org.magemello.sys.node.repository.RecordRepository;
import org.magemello.sys.protocol.raft.Raft;
import org.magemello.sys.protocol.raft.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service("CP")
@SuppressWarnings("rawtypes")
public class CPProtocolService implements ProtocolService {

    private static final Logger log = LoggerFactory.getLogger(CPProtocolService.class);

    @Value("${server.port}")
    private String serverPort;

    @Autowired
    private RecordRepository recordRepository;
    @Autowired
    private P2PService p2pService;
    @Autowired
    private CPProtocolClient client;

    private Raft raft;

    @Override
    public Mono<ResponseEntity> get(String key) {
        return Mono.empty();
    }

    @Override
    public Mono<ResponseEntity> set(String key, String value) throws Exception {
        return Mono.empty();
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
        int majority = 1 + p2pService.getPeers().size() / 2;
        raft = new Raft(Integer.parseInt(serverPort), client, majority);
        raft.start();
    }

    @Override
    public void stop() {
        raft.stop();
    }

    public boolean vote(VoteRequest vote) {
        if (raft.handleVoteRequest(vote)) {
            log.info("- Voting yes");
            return true;
        } else {
            log.info("- Voting no");
            return false;
        }
    }

    public boolean beat(Update update) {
        if (raft.handleBeat(update)) {
            log.info("- Update succeed");
            return true;
        } else {
            log.info("- Update failed");
            return false;
        }
    }
}
