package org.magemello.sys.node.protocols.cp.service;

import org.magemello.sys.node.controller.DemoController;
import org.magemello.sys.node.domain.RecordTerm;
import org.magemello.sys.node.domain.VoteRequest;
import org.magemello.sys.node.protocols.cp.clients.CPProtocolClient;
import org.magemello.sys.node.protocols.cp.domain.Epoch;
import org.magemello.sys.node.protocols.cp.domain.Update;
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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.magemello.sys.node.repository.*;

import static org.magemello.sys.node.protocols.cp.domain.Utils.DEFAULT_TICK_TIMEOUT;
import static org.magemello.sys.node.protocols.cp.domain.Utils.randomize;

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

    private Integer quorum;

    private volatile Epoch epoch;

    private volatile Runnable status;

    private VotingBoard votes;

    private RecordTerm updateBuffer;

//    private String leaderAddress;

    @Override
    public Mono<ResponseEntity> get(String key) {
        Optional<RecordTerm> record = recordRepository.findByKey(key);
        if (record.isPresent()) {
            return Mono.just(ResponseEntity.status(HttpStatus.OK).body("RAFT " + record.get().toString()));
        } else {
            return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
        }
    }

    @Override
    public Mono<ResponseEntity> set(String key, String value) throws Exception {
        if (status == follower) {
            log.info("\nForwarding write request of {} to leader {} for value {}", key, epoch.getLeader(), value);
            ClientResponse clientResponse = cpProtocolClient.forwardDataToLeader(key, value, epoch.getLeader()).block();
            log.info("\nWrite request result: {}\n", clientResponse.statusCode());

            return Mono.just(ResponseEntity.status(clientResponse.statusCode()).build());
        } else if (status == leader) {
            log.info("\nReceived write request of {} for value {}\n", key, value);
            updateBuffer = new RecordTerm(key, value, epoch.getTerm(), epoch.getTick());
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
        this.epoch = new Epoch(0);
        this.votes = new VotingBoard();
        this.status = new Runnable() {
            @Override
            public void run() {
                DemoController.clr();
                status = follower;
            }
        };

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

    public boolean vote(VoteRequest vote) {
        return handleVoteRequest(vote);
    }

    public boolean beat(Update update) {
        return handleBeat(update);
    }

    public boolean handleVoteRequest(VoteRequest vote) {
        epoch.touch();
        return votes.getVote(vote);
    }

    public boolean handleBeat(Update beat) {
        Integer currentTerm = epoch.getTerm();
        Integer currentTick = epoch.getTick();

        boolean success = epoch.update(beat);
        if (success) {
            if (status == candidate) {
                log.info("\nOps! Somebody is already in charge, election aborted!\n");
                switchToFollower();
            } else if (status == leader) {
                log.info("\nOps! Two leaders here? Let's start an election!\n");
                switchToCandidate();
            }
        }

        if ((currentTerm != beat.term && beat.tick != 1) || (currentTerm == beat.term && beat.tick - currentTick > 1)) {
//            log.info("-----Sync {} {} - {} {}", epoch.getTerm(), epoch.getTick(), beat.term, beat.tick);
            log.info("Asking history from term {} and tick {} to {}", epoch.getTerm(), epoch.getTick(), beat.from);
            cpProtocolClient.history(epoch.getTerm(), epoch.getTick(), beat.from).subscribe(recordTerm -> {
                recordRepository.save(recordTerm);
            });
            return true;
        } else {
            if (beat.data != null) {
                recordRepository.save(beat.data);
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
            if (epoch.isExpired()) {
                log.info("\nNo leader is present in term {}: time for an election!", epoch.getTerm());
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
            epoch.nextTick();

            if (updateBuffer != null) {
                recordRepository.save(updateBuffer);
            }

            log.info("\rBeating, term={},tick={}{}", epoch.getTerm(), epoch.getTick(), (updateBuffer != null) ? ",data=" + updateBuffer.toString() + "\n" : "");

            cpProtocolClient.sendBeat(new Update(serverPort, epoch, updateBuffer), quorum).subscribe(responses -> {
                if (responses < quorum) {
                    log.info("\nI was able to end the beat only to {} followers for term {}", responses, epoch.getTerm());
                    switchToFollower();
                }
                updateBuffer = null;
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
        epoch = epoch.nextTerm();
        votes.put(epoch.getTerm(), serverPort);
        switchStatus(candidate);

        cpProtocolClient.requestVotes(epoch.getTerm(), quorum).subscribe(voteQuorum -> {
            if (voteQuorum >= quorum) {
                log.info("\nI was elected leader for term {}!", epoch.getTerm());
                switchStatus(leader);
            }
        });
    }


    private void switchStatus(Runnable newStatus) {
        if (status != newStatus) {
            log.info("\nSwitching from status {} to status {}\n", status, newStatus);
            status = newStatus;
        }
    }

    private void scheduleNext(ScheduledExecutorService scheduler, Runnable runnable) {
        scheduler.schedule(runnable, randomize(DEFAULT_TICK_TIMEOUT / 2), TimeUnit.MILLISECONDS);
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
