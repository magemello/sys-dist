package org.magemello.sys.protocol.raft;

import static org.magemello.sys.protocol.raft.Utils.DEFAULT_TICK_TIMEOUT;
import static org.magemello.sys.protocol.raft.Utils.randomize;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.magemello.sys.node.clients.CPProtocolClient;
import org.magemello.sys.node.domain.VoteRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            log.info("Sending beat, term {}, tick {}", epoch.getTerm(), epoch.getTick());

            api.sendBeat(new Update(whoami, epoch, null), quorum).subscribe(beatQuorum -> {
                if (beatQuorum < quorum) {
                    log.info("It appears I have not enough followers :( for term {}", epoch.getTerm());
                    switchToFollower();
                } else {
                    log.info("I'm still the leader for term {}!", epoch.getTerm());
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
        epoch = epoch.nextTerm();
        votes.put(epoch.getTerm(), whoami);
        switchStatus(candidate);

        api.requestVotes(whoami, epoch.getTerm(), quorum).subscribe(voteQuorum -> {
            if (voteQuorum >= quorum) {
                log.info("I was elected leader for term {}!", epoch.getTerm());
                switchStatus(leader);
            }
        });
    }


    private void switchStatus(Runnable newStatus) {
        log.info("Switching from status {} to status {}", status, newStatus);
        status = newStatus;
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
