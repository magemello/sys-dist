package org.magemello.sys.node.protocols.cp.domain;

public interface Utils {

    public final long DEFAULT_TICK_TIMEOUT = 1000;
    public final long DEFAULT_UPDATE_TIMEOUT = 2500;
    public final long DEFAULT_ELECTION_TIMEOUT = 5000;

    public static long randomize(long value) {
        long third = value/3;
        return 2*third + (long)(Math.random()*third);
    }
}
