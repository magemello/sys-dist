package org.magemello.sys.node.clients;

import org.magemello.sys.node.service.P2PService;
import org.magemello.sys.protocol.raft.Epoch;
import org.magemello.sys.protocol.raft.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CPProtocolClient {

    private static final Logger log = LoggerFactory.getLogger(CPProtocolClient.class);

    @Autowired
    private P2PService p2pService;

    public int requestVotes(int whoami) {
        // this call should send a vote request to all the peers
        // for each peer answering, a vote is awarded
        // the total number of received votes is returned
        // a timeout in a call means a negative vote
        return 0;
    }

    public void sendBeat(Update update) {
        // TODO Auto-generated method stub
        
    }
}
