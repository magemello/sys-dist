package org.magemello.sys.node.clients;

import org.magemello.sys.node.service.P2PService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class APProtocolClient {

    @Autowired
    private P2PService p2pService;

}
