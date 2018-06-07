package org.magemello.sys.node.protocols.cp.service;

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
    private RecordTermRepository recordTermRepository;

    @Autowired
    private CPProtocolClient cpProtocolClient;

    private Integer quorum;


    private volatile Epoch epoch;
    private volatile Runnable status;

    private VotingBoard votes;

    private RecordTerm dataBuffer;


//    private String leaderAddress;

    @Override
    public Mono<ResponseEntity> get(String key) {
        Optional<RecordTerm> record = recordTermRepository.findByKey(key);
        if (record.isPresent()) {
            return Mono.just(ResponseEntity.status(HttpStatus.OK).body("QUORUM " + record.get().toString()));
        } else {
            return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
        }
    }

    @Override
    public Mono<ResponseEntity> set(String key, String value) throws Exception {
        if (status == follower) {
            log.info("- Forwarding write request of {} to leader {} for value {}", key, epoch.getLeader(), value);

            ClientResponse clientResponse = cpProtocolClient.forwardDataToLeader(key, value, epoch.getLeader()).block();
            log.info("- Status write request {} ", clientResponse.statusCode());

            return Mono.just(ResponseEntity.status(clientResponse.statusCode()).build());
        } else if (status == leader) {
            log.info("- Receive write request of {} for value {}", key, value);
            return Mono.just(ResponseEntity.status(HttpStatus.OK).build());
        } else {
            log.info("- No leader elected yet");
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("No leader at the moment!"));
        }

//        if (amITheLeader()) {
//            log.info("- I'm the leader scheduling data for the next beat");
//
//            if (setData(key, value)) {
//                log.info("- Data scheduled");
//
//                return Mono.just(ResponseEntity.status(HttpStatus.OK).build());
//            } else {
//                log.info("- Buffer full data not scheduled");
//
//                return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build());
//            }
//        }
//
//        if (amIAFollower()) {
//            log.info("- Forwarding write request of {} to leader {} for value {}", key, leaderAddress, value);
//
//            ClientResponse clientResponse = cpProtocolClient.forwardDataToLeader(key, value, leaderAddress).block();
//            log.info("- Status write request {} ", clientResponse.statusCode());
//
//            return Mono.just(ResponseEntity.status(clientResponse.statusCode()).build());
//        }

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
        this.quorum = 1 + p2pService.getPeers().size() / 2;
        this.epoch = new Epoch(0);
        this.status = follower;
        this.votes = new VotingBoard();

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduleNext(scheduler, new Runnable() {
            @Override
            public void run() {
                if (status != null) {
                    status.run();
                    scheduleNext(scheduler, this);
                } else {
                    log.info("Shutting down");
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
        boolean success = epoch.update(beat);
        if (success) {
            if (status == candidate) {
                log.info("Ops! Somebody is already in charge, election aborted!");
                switchToFollower();
            } else if (status == leader) {
                log.info("Ops! Two leaders here? Let's start an election!");
                switchToCandidate();
            }
        }
        return success;

//        boolean success = epoch.update(beat);
//        if (success) {
//            if (status == candidate) {
//                log.info("Ops! Somebody is already in charge, election aborted!");
//                switchToFollower();
//            } else if (amITheLeader()) {
//                log.info("Ops! Two leaders here? Let's start an election!");
//                switchToCandidate();
//                return false;
//            }
//        } else {
//            return false;
//        }
//
//        if ((epoch.getTerm() != beat.term && beat.tick != 1) || (epoch.getTerm() == beat.term && beat.tick - epoch.getTick() > 1)) {
//            log.info("Asking history from term {} and tick {} to {}", epoch.getTerm(), epoch.getTick(), beat.from);
//            String leaderAddress = serverAddress + beat.from.toString();
//
//            cpProtocolClient.history(epoch.getTerm(), epoch.getTick(), leaderAddress).subscribe(recordTerm -> {
//                recordTermRepository.save(recordTerm);
//            });
//            return true;
//        } else {
//            recordTermRepository.save(beat.data);
//        }
//
//        return true;
    }

    public boolean amITheLeader() {
        return status == leader;
    }

    public boolean amIAFollower() {
        return status == follower;
    }

    public boolean setData(String key, String value) {
        if (dataBuffer == null) {
            dataBuffer = new RecordTerm(key, value);
            return true;
        } else {
            return false;
        }
    }

    private Runnable follower = new Runnable() {
        @Override
        public void run() {
            if (epoch.isExpired()) {
                log.info("No leader is present in term {}: time for an election!", epoch.getTerm());
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
                log.info("Nothing happening, let's try another election!");
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
            log.info("Sending beat, term {}, tick {}", epoch.getTerm(), epoch.getTick());
            if (dataBuffer != null) {
                dataBuffer.setTermAndTick(epoch.getTerm(), epoch.getTick());
                recordTermRepository.save(dataBuffer);
            }

            cpProtocolClient.sendBeat(new Update(serverPort, epoch, dataBuffer), quorum).subscribe(responses -> {
                if (responses < quorum) {
                    log.info("I was able to end the beat only to {} followers for term {}", responses, epoch.getTerm());
                    switchToFollower();
                }
                dataBuffer = null;
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
                log.info("I was elected leader for term {}!", epoch.getTerm());
                switchStatus(leader);
            }
        });
    }


    private void switchStatus(Runnable newStatus) {
        if (status != newStatus) {
            log.info("Switching from status {} to status {}", status, newStatus);
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
