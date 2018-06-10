package org.magemello.sys.node.protocols.cp.service;

import static org.magemello.sys.node.protocols.cp.domain.Utils.DEFAULT_TICK_TIMEOUT;
import static org.magemello.sys.node.protocols.cp.domain.Utils.randomize;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.magemello.sys.node.domain.Record;
import org.magemello.sys.node.protocols.cp.clients.CPProtocolClient;
import org.magemello.sys.node.protocols.cp.domain.CPRecord;
import org.magemello.sys.node.protocols.cp.domain.Epoch;
import org.magemello.sys.node.protocols.cp.domain.Update;
import org.magemello.sys.node.protocols.cp.domain.VoteRequest;
import org.magemello.sys.node.repository.RecordRepository;
import org.magemello.sys.node.service.P2PService;
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
    private Integer serverPort;

    @Value("${server.address}")
    private String serverAddress;

    @Autowired
    private P2PService p2pService;

    @Autowired
    private RecordRepository recordRepository;

    @Autowired
    private CPProtocolClient cpProtocolClient;

    private volatile Epoch clock;
    private volatile Runnable status;

    private Integer quorum;
    private int electionTerm;
    private VotingBoard votes;

    private volatile CPRecord updateBuffer;

    @Override
    public Mono<ResponseEntity> get(String key) {
        Optional<Record> record = recordRepository.findByKey(key);
        if (record.isPresent()) {
            return Mono.just(ResponseEntity.status(HttpStatus.OK).body("RAFT " + record.get().toString()));
        } else {
            return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
        }
    }

    @Override
    public Mono<ResponseEntity> set(String key, String value) throws Exception {
        if (status == follower) {
            log.info("\nForwarding write request of {} to leader {} for value {}", key, clock.getLeader(), value);
            ClientResponse clientResponse = cpProtocolClient.forwardDataToLeader(key, value, clock.getLeader()).block();
            log.info("\nWrite request result: {}\n", clientResponse.statusCode());

            return Mono.just(ResponseEntity.status(clientResponse.statusCode()).build());
        } else if (status == leader) {
            log.info("\nReceived write request of {} for value {}\n", key, value);
            updateBuffer = new CPRecord(key, value, clock.getTerm(), clock.getTick());
            return Mono.just(ResponseEntity.status(HttpStatus.OK).build());
        } else {
            log.info("\nNo leader elected yet\n");
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("No leader at the moment!"));
        }
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
        log.info("\nCP mode (majority quorum, raft)\n");
        this.quorum = 1 + p2pService.getPeers().size() / 2;
        this.clock = new Epoch(0);
        this.votes = new VotingBoard();
        this.status = follower;

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduleNext(scheduler, new Runnable() {
            @Override
            public void run() {
                if (status != null) {
                    status.run();
                    scheduleNext(scheduler, this);
                } else {
                    log.info("\nShutting down");
                }
            }
        });
    }

    @Override
    public void stop() {
        status = null;
        try {
            Thread.sleep(DEFAULT_TICK_TIMEOUT);
        } catch (InterruptedException e) {
            Thread.interrupted();
        }
    }

    public boolean handleVoteRequest(VoteRequest vote) {
        if (status == leader || status == follower) {
            return false;
        }
        clock.touch();

        boolean res = votes.getVote(vote);
        log.info("\n/vote request from {}, term {}: {}", vote.getPort(), vote.getTerm(), res ? "yes" : "no");
        return res;
    }

    public boolean handleBeat(Update beat) {
        Integer currentTerm = clock.getTerm();
        Integer currentTick = clock.getTick();

        boolean success = clock.update(beat);
        if (success) {
            if (status == candidate) {
                log.info("\nOps! Somebody is already in charge, election aborted!\n");
                switchToFollower();
            } else if (status == leader) {
                log.info("\nOps! Two leaders here? Let's start an election!\n");
                switchToCandidate();
            }

            if (beat.data != null) {
                recordRepository.save(beat.data);
            }

            electionTerm = currentTerm;
        }

        if ((currentTerm != beat.term && beat.tick != 1) || (currentTerm == beat.term && beat.tick - currentTick > 1)) {
            log.info("\nAsking history from term {} and tick {} to {}\n", clock.getTerm(), clock.getTick(), beat.from);
            cpProtocolClient.history(clock.getTerm(), clock.getTick(), beat.from).subscribe(record -> {
                log.info("\n- history: {}\n", record);
                recordRepository.save(record);
            });
            return true;
        } else {
            log.info("\r/update {}            ", beat.toCompactString());
            if (beat.data != null) {
                log.info("\n- with data: {}\n", beat.data);
            }
        }

        return success;
    }

    public boolean amITheLeader() {
        return status == leader;
    }

    public boolean amIAFollower() {
        return status == follower;
    }

    private Runnable follower = new Runnable() {
        @Override
        public void run() {
            if (clock.isExpired()) {
                log.info("\nNo leader is present in term {}: time for an election!", clock.getTerm());
                switchToCandidate();
            }
        }

        @Override
        public String toString() {
            return "follower";
        }
    };

    private Runnable candidate = new Runnable() {
        private int count;

        @Override
        public void run() {
            if (++count % 10 == 0) {
                log.info("\nNothing happening, let's try another election!");
                switchToCandidate();
            }
        }

        @Override
        public String toString() {
            return "candidate";
        }
    };

    private Runnable leader = new Runnable() {
        @Override
        public void run() {
            clock.nextTick();

            CPRecord localBuffer = updateBuffer;
            updateBuffer = null;

            log.info("\rBeating, term={},tick={}", clock.getTerm(), clock.getTick());
            if (localBuffer != null) {
                recordRepository.save(localBuffer);
                log.info("\n- sending data: {}\n", localBuffer);
            }

            cpProtocolClient.sendBeat(new Update(serverPort, clock, localBuffer), quorum).subscribe(responses -> {
                if (responses < quorum) {
                    log.info("\nI was able to end the beat only to {} followers for term {}", responses, clock.getTerm());
                    switchToFollower();
                }
            });
        }

        @Override
        public String toString() {
            return "leader";
        }
    };

    private void switchToFollower() {
        switchStatus(follower);
    }

    private void switchToCandidate() {
        electionTerm++;
        votes.put(electionTerm, serverPort);
        switchStatus(candidate);

        cpProtocolClient.requestVotes(electionTerm, quorum).subscribe(voteQuorum -> {
            if (voteQuorum >= quorum) {
                log.info("\nI was elected leader for term {}!", clock.getTerm());
                switchToLeader();
            }
        });
    }

    private void switchToLeader() {
        clock = new Epoch(electionTerm);
        switchStatus(leader);
    }


    private void switchStatus(Runnable newStatus) {
        int term = Math.max(electionTerm, clock.getTerm());
        if (status != newStatus) {
            log.info("\nSwitching from status {} to status {} in term {}\n\n", status, newStatus, term);
            status = newStatus;
        } else {
            log.info("\nStatus {} in term {}\n", status, term);
        }
    }

    private void scheduleNext(ScheduledExecutorService scheduler, Runnable runnable) {
        scheduler.schedule(runnable, randomize(DEFAULT_TICK_TIMEOUT / 2), TimeUnit.MILLISECONDS);
    }

    public ArrayList<CPRecord> getHistory(Integer term, Integer tick) {
        return recordRepository.findByTermLessThanEqualAndTickLessThanEqual(term, tick);
    }
}

class VotingBoard {

    private Map<Integer, Integer> board = new HashMap<>();

    public synchronized boolean getVote(VoteRequest voteRequest) {
        Integer term = voteRequest.getTerm();
        Integer vote = board.get(term);
        if (vote == null) {
            put(term, voteRequest.getPort());
            return true;
        } else {
            return false;
        }
    }

    public void put(Integer term, Integer from) {
        board.put(term, from);
    }

}
