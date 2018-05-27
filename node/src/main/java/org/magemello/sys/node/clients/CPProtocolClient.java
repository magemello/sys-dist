package org.magemello.sys.node.clients;

import org.magemello.sys.node.service.P2PService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CPProtocolClient {

    private static final Logger log = LoggerFactory.getLogger(CPProtocolClient.class);

    @Autowired
    private P2PService p2pService;

}
