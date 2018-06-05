package org.magemello.sys.protocol.raft;

import static org.magemello.sys.protocol.raft.Utils.DEFAULT_TICK_TIMEOUT;
import static org.magemello.sys.protocol.raft.Utils.randomize;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.magemello.sys.node.clients.CPProtocolClient;
import org.magemello.sys.node.service.CPProtocolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Raft {

    private static final Logger log = LoggerFactory.getLogger(Raft.class);

    private final int whoami;
    private final int quorum;
    private final CPProtocolClient api;
    
    private volatile Epoch epoch;
    private volatile Runnable status;

    
    public Raft(int whoami, CPProtocolClient api, int quorum) {
        this.api = api;
        this.whoami = whoami;
        this.epoch = new Epoch(0);
        this.quorum = quorum;
        this.status = follower;
    }
    
    private Runnable follower = new Runnable() {
        @Override
        public void run() {
            if (epoch.isExpired()) {
                log.info("No leader is present in term {}: time for an election!", epoch.getTerm());
                epoch = epoch.nextTerm();
                switchStatus(candidate);
                api.requestVotes(whoami);
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
        }
        @Override
        public String toString() {
            return "leader";
        }
    };

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
        scheduler.schedule(runnable, randomize(DEFAULT_TICK_TIMEOUT/2), TimeUnit.MILLISECONDS);
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
