package org.magemello.sys.node.service;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class P2PService {

    private List<String> peers = Arrays.asList("http://localhost:9999/", "http://localhost:7777/");

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
