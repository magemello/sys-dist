package org.magemello.sys.node.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class P2PService {

    @Value("${server.port}")
    private String serverPort;

    @Value("#{'${peers}'.split(',')}")
    private List<String> peers;

    @PostConstruct
    public void init() {
        this.peers = peers.stream().filter(port -> !port.equals(serverPort)).collect(Collectors.toList());
    }

    public List<String> getPeers() {
        return peers;
    }

    public void addPeer(String peer) {
        peers.add(peer);
    }

    public void clear() {
        peers.clear();
    }

}
