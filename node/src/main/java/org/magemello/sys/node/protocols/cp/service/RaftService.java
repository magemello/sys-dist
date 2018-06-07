package org.magemello.sys.node.protocols.cp.service;

import org.magemello.sys.node.domain.RecordTerm;
import org.magemello.sys.node.domain.VoteRequest;
import org.magemello.sys.node.protocols.cp.clients.CPProtocolClient;
import org.magemello.sys.node.protocols.cp.raft.Epoch;
import org.magemello.sys.node.protocols.cp.raft.Update;
import org.magemello.sys.node.repository.RecordTermRepository;
import org.magemello.sys.node.service.P2PService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.magemello.sys.node.protocols.cp.raft.Utils.DEFAULT_TICK_TIMEOUT;
import static org.magemello.sys.node.protocols.cp.raft.Utils.randomize;

@Service
public class RaftService {

    private static final Logger log = LoggerFactory.getLogger(RaftService.class);


    @Value("${server.port}")
    private Integer serverPort;

    @Value("${server.address}")
    private String serverAddress;

    @Autowired
    private P2PService p2pService;

    @Autowired
    private CPProtocolClient api;

    @Autowired
    private RecordTermRepository recordTermRepository;

    private Integer quorum;


    private volatile Epoch epoch;
    private volatile Runnable status;

    private VotingBoard votes;

    private RecordTerm dataBuffer;


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
            } else if (amITheLeader()) {
                log.info("Ops! Two leaders here? Let's start an election!");
                switchToCandidate();
                return false;
            }
        } else {
            return false;
        }

        if ((epoch.getTerm() != beat.term && beat.tick != 1) || (epoch.getTerm() == beat.term && beat.tick - epoch.getTick() > 1)) {
            log.info("Asking history from term {} and tick {} to {}", epoch.getTerm(), epoch.getTick(), beat.from);
            String leaderAddress = serverAddress + beat.from.toString();

            api.history(epoch.getTerm(), epoch.getTick(), leaderAddress).subscribe(recordTerm -> {
                recordTermRepository.save(recordTerm);
            });
            return true;
        } else {
            recordTermRepository.save(beat.data);
        }
        return false;
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
            dataBuffer.setTermAndTick(epoch.getTerm(), epoch.getTick());
            recordTermRepository.save(dataBuffer);

            api.sendBeat(new Update(serverPort, epoch, dataBuffer), quorum).subscribe(responses -> {
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

        api.requestVotes(epoch.getTerm(), quorum).subscribe(voteQuorum -> {
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

    public void start() {
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
