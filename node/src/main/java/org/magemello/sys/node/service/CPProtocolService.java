package org.magemello.sys.node.service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.magemello.sys.node.domain.Record;
import org.magemello.sys.node.repository.RecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service("CP")
@SuppressWarnings("rawtypes")
public class CPProtocolService implements ProtocolService {

    private static final Logger log = LoggerFactory.getLogger(CPProtocolService.class);

    private final long DEFAULT_TICK_TIMEOUT = 1000;
    private final long DEFAULT_UPDATE_TIMEOUT = 2500;
    private final long DEFAULT_ELECTION_TIMEOUT = 5000;
        
    @Autowired
    private RecordRepository recordRepository;
    @Autowired
    private P2PService p2pService;

    private volatile Runnable status;

    private int majorityQuorum;
    private long endOfTermTime;
    
    private int currentTerm = 0;
    private int currentTick = 0;
    private int currentLeader = 0;
    
    private void scheduleNext(ScheduledExecutorService scheduler, Runnable runnable) {
        scheduler.schedule(runnable, randomized(DEFAULT_TICK_TIMEOUT/2), TimeUnit.MILLISECONDS);
    }
    
    @Override
    public Mono<ResponseEntity> get(String key) {
        recordRepository.findByKey(key);
        return Mono.empty();
    }

    @Override
    public Mono<ResponseEntity> set(String key, String value) throws Exception {
        recordRepository.save(new Record(key, value));
        return Mono.empty();
    }

    @Override
    public String protocolName() {
        return "CP";
    }

    @Override
    public void reset(){

    }

    private Runnable follower = new Runnable() {
        @Override
        public void run() {
            if (System.currentTimeMillis() > endOfTermTime) {
                log.info("No leader is present in term {}: time for an election!", currentTerm);
                currentLeader = 0;
                
            }
        }
        @Override
        public String toString() {
            return "follower";
        }
    };

    private Runnable candidate = new Runnable() {
        @Override
        public void run() {
            // TODO Auto-generated method stub
            
        }
        @Override
        public String toString() {
            return "candidate";
        }
    };

    private Runnable leader = new Runnable() {
        @Override
        public void run() {
            // TODO Auto-generated method stub
            
        }
        @Override
        public String toString() {
            return "leader";
        }
    };

    private long randomized(long value) {
        return value + (long) (Math.random()*(value/3));
    }

    @Override
    public void start() {
        log.info("CP mode (majority quorum, raft)");
        status = follower;
        majorityQuorum = p2pService.getPeers().size()/2;

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduleNext(scheduler, new Runnable() {
            @Override
            public void run() {
                if (status != null) {
                    status.run();
                    scheduleNext(scheduler, this);
                }
            }
        });
    }

    @Override
    public void stop() {
        status = null;

        try {
            Thread.sleep(DEFAULT_TICK_TIMEOUT);
        } catch (InterruptedException e) {
            Thread.interrupted();
        }
    }


}
