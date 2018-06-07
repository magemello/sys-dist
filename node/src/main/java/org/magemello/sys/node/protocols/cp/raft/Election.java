package org.magemello.sys.node.protocols.cp.raft;

import java.util.HashSet;
import java.util.Set;

public class Election {

    private final int term;
    private final Set<Integer> votes;

    public Election(int term) {
        this.term = term;
        this.votes = new HashSet<>();
    }

    public int term() {
        return term;
    }

    public void registerVote(int from) {
        votes.add(from);
    }
}
