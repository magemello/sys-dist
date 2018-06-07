package org.magemello.sys.node.protocols.cp.domain;

import org.magemello.sys.node.domain.RecordTerm;

public class Update {

    public Integer from;
    public Integer term;
    public Integer tick;
    public RecordTerm data;

    protected Update() {}

    public Update(int whoami, Epoch epoch, RecordTerm data) {
        this.from = whoami;
        this.term = epoch.getTerm();
        this.tick = epoch.getTick();
        this.data = data;
    }

    @Override
    public String toString() {
        return  "{"
                + "from=" + from +
                ",term=" + term +
                ",tick=" + tick +
                ",data=" + data +
                '}';
    }

    public String toCompactString() {
        return  "{"
                + "from=" + from +
                ",term=" + term +
                ",tick=" + tick +
                '}';
    }
}
