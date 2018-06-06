package org.magemello.sys.protocol.raft;

import org.magemello.sys.node.domain.Record;

public class Update {

    public int from;
    public int term;
    public int tick;
    public Record data;

    protected Update() {}
    
    public Update(int whoami, Epoch epoch, Record data) {
        this.from = whoami;
        this.term = epoch.getTerm();
        this.tick = epoch.getTick();
        this.data = data;
    }

    @Override
    public String toString() {
        return  "{"
                + "from=" + from +
                ", term=" + term +
                ", tick=" + tick +
                ", data=" + data +
                '}';
    }
}
