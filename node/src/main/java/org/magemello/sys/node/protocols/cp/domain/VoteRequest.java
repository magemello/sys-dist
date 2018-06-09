package org.magemello.sys.node.protocols.cp.domain;

public class VoteRequest {

    private Integer port;

    private Integer term;

    public VoteRequest() {
    }

    public VoteRequest(Integer port, Integer term) {
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
