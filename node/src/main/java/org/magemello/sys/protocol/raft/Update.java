package org.magemello.sys.protocol.raft;

import org.magemello.sys.node.domain.Record;

public class Update {

    public final int from;
    public final int term;
    public final int tick;
    public final Record data;
    
    public Update(int whoami, Epoch epoch, Record data) {
        this.from = whoami;
        this.term = epoch.getTerm();
        this.tick = epoch.getTick();
        this.data = data; 
    }
}
