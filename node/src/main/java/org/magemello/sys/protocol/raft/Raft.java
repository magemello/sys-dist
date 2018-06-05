package org.magemello.sys.protocol.raft;

import static org.magemello.sys.protocol.raft.Utils.DEFAULT_TICK_TIMEOUT;
import static org.magemello.sys.protocol.raft.Utils.randomize;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.magemello.sys.node.clients.CPProtocolClient;
import org.magemello.sys.node.domain.VoteRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.ClientResponse;

import reactor.core.CoreSubscriber;
import reactor.core.publisher.Mono;

public class Raft {

    private static final Logger log = LoggerFactory.getLogger(Raft.class);

    private final int whoami;
    private final int quorum;
    private final CPProtocolClient api;

    private volatile Epoch epoch;
    private volatile Runnable status;

    private VotingBoard votes;

    public Raft(int whoami, CPProtocolClient api, int quorum) {
        this.api = api;
        this.whoami = whoami;
        this.epoch = new Epoch(0);
        this.quorum = quorum;
        this.status = follower;
        this.votes = new VotingBoard();
    }

    public boolean handleVoteRequest(VoteRequest vote) {
        epoch.touch();
        return votes.getVote(vote);
    }

    public boolean handleBeat(Update update) {
        boolean success = epoch.update(update);
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
        @Override
        public void run() {
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
            api.sendBeat(new Update(whoami, epoch, null));

            int responses = 0;
            if (responses < quorum) {
                log.info("It appears I have not enough followers, just {} responded to my update", responses);
                switchToFollower();
            }

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
        switchStatus(candidate);


        handleVote(whoami, epoch.getTerm());
    }

    private Mono<ResponseEntity> handleVote(Integer whoami, Integer term) {
        return new Mono<ResponseEntity>() {

            private CoreSubscriber<? super ResponseEntity> actual;

            AtomicInteger voteQuorum = new AtomicInteger(0);

            @Override
            public void subscribe(CoreSubscriber<? super ResponseEntity> actual) {
                log.info("Sending vote for request to peers");

                this.actual = actual;

                api.requestVotes(whoami, term)
                        .map(this::manageRequestVoteQuorum).collectList()
                        .doFinally(signalType -> actual.onComplete());
            }

            private ClientResponse manageRequestVoteQuorum(ClientResponse clientResponse) {
                if (!clientResponse.statusCode().isError()) {
                    if (voteQuorum.incrementAndGet() >= quorum) {
                        log.info("I was elected leader for term {}!", term);
                        switchStatus(leader);
                        actual.onComplete();
                    }
                }

                return clientResponse;
            }
        };
    }

    private void switchStatus(Runnable newStatus) {
        log.info("Switching from status {} to status {}", status, newStatus);
        newStatus = status;
    }

    public void start() {
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

    private void scheduleNext(ScheduledExecutorService scheduler, Runnable runnable) {
        scheduler.schedule(runnable, randomize(DEFAULT_TICK_TIMEOUT / 2), TimeUnit.MILLISECONDS);
    }

    public void stop() {
        status = null;

        try {
            Thread.sleep(DEFAULT_TICK_TIMEOUT);
        } catch (InterruptedException e) {
            Thread.interrupted();
        }
    }
}

class VotingBoard {

    private Map<Integer, Integer> board = new HashMap<>();

    public synchronized boolean getVote(VoteRequest voteRequest) {
        Integer vote = board.get(voteRequest.getTerm());
        if (vote == null) {
            board.put(voteRequest.getTerm(), voteRequest.getPort());
            return true;
        } else {
            return false;
        }
    }

}
