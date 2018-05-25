package org.magemello.sys.node.service;

import org.magemello.sys.node.clients.ACProtocolClient;
import org.magemello.sys.node.domain.Record;
import org.magemello.sys.node.repository.RecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Service("AC")
public class ACProtocolService implements ProtocolService {

    private static final Logger log = LoggerFactory.getLogger(ACProtocolService.class);

    @Autowired
    private RecordRepository recordRepository;

    @Autowired
    private ACProtocolClient acProtocolClient;

    private Map<String, Record> writeAheadLog = new LinkedHashMap<>();

    private Set<String> keys = new HashSet<>();

    @Override
    public Mono<ResponseEntity> get(String key) {
        log.info("AC Service - get for {} ", key);

        return handleGet(key);
    }

    @Override
    public Mono<ResponseEntity> set(String key, String value) throws Exception {
        log.info("AC Service - Proposing to peers");
        Record record = new Record(key, value);

        return handleSet(record);
    }

    @Override
    public String protocolName() {
        return "AC";
    }

    public boolean propose(Record record) {
        log.info("AC Service - Propose for {} ", record);

        if (!keys.contains(record.getKey())) {
            keys.add(record.getKey());
            writeAheadLog.put(record.get_ID(), record);
            return true;
        }
        return false;
    }

    public Record commit(String id) {
        log.info("AC Service - Commit id {} ", id);

        Record record = writeAheadLog.get(id);

        if (record != null) {
            recordRepository.save(record);
            writeAheadLog.remove(id);
            keys.remove(record.getKey());
        }
        return record;
    }

    public Record rollback(String id) {
        log.info("AC Service - Rollback id {} ", id);

        Record record = writeAheadLog.get(id);

        if (record != null) {
            writeAheadLog.remove(id);
            keys.remove(record.getKey());
        }
        return record;
    }

    public void clearWriteHeadLog() {
        writeAheadLog.clear();
        keys.clear();
    }

    private Mono<ResponseEntity> handleGet(String key) {
        return new Mono<ResponseEntity>() {
            @Override
            public void subscribe(CoreSubscriber<? super ResponseEntity> actual) {
                actual.onNext(ResponseEntity.ok().body(recordRepository.findByKey(key).toString()));
                actual.onComplete();
            }
        };
    }

    private Mono<ResponseEntity> handleSet(Record record) {
        return new Mono<ResponseEntity>() {

            CoreSubscriber<? super ResponseEntity> actual;

            @Override
            public void subscribe(CoreSubscriber<? super ResponseEntity> actual) {
                this.actual = actual;

                acProtocolClient.propose(record)
                        .doOnError(this::handleProposeError).log("Error -> Sending rollback to peers")
                        .subscribe(this::handleProposeResult);
            }

            private void handleProposeResult(Boolean resultVote) {
                if (resultVote) {
                    log.info("Propose for {} succeed sending commit to peers", record);

                    acProtocolClient.commit(record.get_ID())
                            .doOnError(this::handleError).log("Error executing commit")
                            .subscribe(this::handleCommitResult);
                } else {
                    log.error("Propose for {} failed sending rollback to peers", record);

                    acProtocolClient.rollback(record.get_ID())
                            .doOnError(this::handleError).log("Error executing rollback")
                            .subscribe(this::handleRollBackResult);
                }
            }

            private void handleProposeError(Throwable error) {
                log.error("Propose for {} failed sending rollback to peers", record);

                acProtocolClient.rollback(record.get_ID())
                        .doOnError(this::handleError).log("Error executing rollback")
                        .subscribe(this::handleRollBackResult);
            }

            private void handleCommitResult(Boolean resultCommit) {
                log.info("Peers Committed {}", record);

                recordRepository.save(record);

                actual.onNext(ResponseEntity
                        .status(HttpStatus.OK)
                        .body("Stored " + record.toString()));
                actual.onComplete();
            }

            private void handleRollBackResult(Boolean RollBack) {
                log.info("Peers Rolled Back {}", record);

                recordRepository.delete(record);

                actual.onNext(ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body("Roll Backed " + record.toString()));
                actual.onComplete();
            }

            private void handleError(Throwable error) {
                actual.onNext(ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .build());
                actual.onComplete();
            }
        };
    }
}
