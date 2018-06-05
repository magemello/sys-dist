package org.magemello.sys.node.domain;

public class Vote {

    Integer port;

    Integer term;

    public Vote() {
    }

    public Vote(Integer port, Integer term) {
        this.port = port;
        this.term = term;
    }

    public Integer getPort() {
        return port;
    }

    public Integer getTerm() {
        return term;
    }

    @Override
    public String toString() {
        return "Vote{" +
                "port=" + port +
                ", term=" + term +
                '}';
    }
}
