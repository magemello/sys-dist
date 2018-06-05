package org.magemello.sys.protocol.raft;

import static org.magemello.sys.protocol.raft.Utils.DEFAULT_ELECTION_TIMEOUT;
import static org.magemello.sys.protocol.raft.Utils.randomize;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Epoch {

    public enum Result {allgood, discard, restart};
    
    private static final Logger log = LoggerFactory.getLogger(Epoch.class);

    private int term;
    private int tick;
    private int leader;
    private long end;
    
    public Epoch(int number) {
        this.term = number;
        updateExpiryTime();
    }
    
    public Result handleTick(Update update) {
        if (update.term < term || update.term == term && update.tick <= tick) {
            log.debug("Received a too old term {}, we are in {}", update.term, term);
            return Result.discard;
        }
        
        if (update.term > term) {
            log.debug("Received a tick in the future - need to restart!");
            return Result.restart;
        }
        
        this.tick = update.tick;
        this.leader = update.from;
        updateExpiryTime();

        return Result.allgood;
    }

    public int getTerm() {
        return term;
    }

    public int getTick() {
        return tick;
    }

    public void nextTick() {
        tick++;
    }

    public Epoch nextTerm() {
        return new Epoch(term+1);
    }

    public boolean isExpired() {
        if (System.currentTimeMillis() > end) {
            log.debug("Timeout expired, need to move on");
            return true;
        } else {
            return false;
        }
    }

    private void updateExpiryTime() {
        this.end = System.currentTimeMillis() + randomize(DEFAULT_ELECTION_TIMEOUT);
    }

}
