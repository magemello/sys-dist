package org.magemello.sys.protocol.raft;

import static org.magemello.sys.protocol.raft.Utils.DEFAULT_ELECTION_TIMEOUT;
import static org.magemello.sys.protocol.raft.Utils.randomize;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Epoch {
    
    private static final Logger log = LoggerFactory.getLogger(Epoch.class);

    private int term;
    private int tick;
    private int leader;
    private long end;
    
    public Epoch(int number) {
        this.term = number;
        touch();
    }
    
    public boolean update(Update update) {
        if (update.term < term || update.term == term && update.tick <= tick) {
            log.info("Received a too old term {}, we are in {}", update.term, term);
            return false;
        }
        
        this.tick = update.tick;
        this.leader = update.from;
        touch();
        
        return true;
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

    public void touch() {
        this.end = System.currentTimeMillis() + randomize(DEFAULT_ELECTION_TIMEOUT);
    }

}
