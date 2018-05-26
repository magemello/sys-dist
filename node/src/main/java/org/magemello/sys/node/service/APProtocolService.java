package org.magemello.sys.node.service;

import org.magemello.sys.node.clients.APProtocolClient;
import org.magemello.sys.node.domain.Record;
import org.magemello.sys.node.repository.RecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Mono;

import java.util.*;

@Service("AP")
@RefreshScope
public class APProtocolService implements ProtocolService {

    private static final Logger log = LoggerFactory.getLogger(APProtocolService.class);

    @Autowired
    RecordRepository recordRepository;

    @Autowired
    private APProtocolClient apProtocolClient;

    private Map<String, Record> writeAheadLog = new LinkedHashMap<>();

    private Set<String> keys = new HashSet<>();

    @Value("${read-quorum:2}")
    private Long readQuorum;

    @Value("${write-quorum:1}")
    private Long writeQuorum;

    @Override
    public Mono<ResponseEntity> get(String key) {
        log.info("AP Service - get for {} ", key);

        HashMap<Record, Integer> matches = new HashMap<>();
        List<Record> records = apProtocolClient.read(key).collectList().block();
        for (Record record : records) {
            Integer counter = matches.get(record);
            if (counter != null) {
                counter++;
                if (counter >= writeQuorum) {
                    apProtocolClient.repair(record).subscribe(aBoolean -> {
                        log.info("Repair outcome " + aBoolean);
                    });
                    return Mono.just(ResponseEntity
                            .status(HttpStatus.OK)
                            .body(record.toString()));
                }
                matches.put(record, counter);
            } else {
                matches.put(record, 1);
            }
        }

        log.info("Disagreement - Sending repair");
        Record repair = recordRepository.findByKey(key);
        if (repair == null) {
            repair = matches.entrySet().iterator().next().getKey();
            if (repair == null) {
                return Mono.just(ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Disagreement - Termonucleare "));
            }
        }
        apProtocolClient.repair(repair).subscribe(aBoolean -> {
            log.info("Repair outcome " + aBoolean);
        });
        return Mono.just(ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Disagreement - repairing with " + repair.toString()));
    }

    @Override
    public Mono<ResponseEntity> set(String key, String value) throws Exception {
        log.info("AP Service - Proposing to peers");
        Record record = new Record(key, value);

        return handleSet(record);
    }

    @Override
    public String protocolName() {
        return "AP";
    }


    public boolean propose(Record record) {
        log.info("AP Service - Propose for {} ", record);

        if (!keys.contains(record.getKey())) {
            keys.add(record.getKey());
            writeAheadLog.put(record.get_ID(), record);
            return true;
        }
        return false;
    }

    public Record commit(String id) {
        log.info("AP Service - Commit id {} ", id);

        Record record = writeAheadLog.get(id);

        if (record != null) {
            recordRepository.save(record);
            writeAheadLog.remove(id);
            keys.remove(record.getKey());
        }
        return record;
    }

    public Record rollback(String id) {
        log.info("AP Service - Rollback id {} ", id);

        Record record = writeAheadLog.get(id);

        if (record != null) {
            writeAheadLog.remove(id);
            keys.remove(record.getKey());
        }
        return record;
    }

    public Record repair(Record record) {
        log.info("AP Service - Repair id {} ", record);

        return recordRepository.save(record);
    }

    public Record read(String key) {
        log.info("AP Service - read record for key {} ", key);
        return recordRepository.findByKey(key);
    }

    public void clearWriteHeadLog() {
        writeAheadLog.clear();
        keys.clear();
    }

    private Mono<ResponseEntity> handleSet(Record record) {
        return new Mono<ResponseEntity>() {

            CoreSubscriber<? super ResponseEntity> actual;

            @Override
            public void subscribe(CoreSubscriber<? super ResponseEntity> actual) {
                log.info("Sending proposal for {} to peers", record);

                this.actual = actual;

                apProtocolClient.propose(record)
                        .subscribe(this::handlePropose,
                                this::handleError);
            }

            private void handlePropose(Long quorum) {
                if (quorum >= writeQuorum) {
                    log.info("Propose for {} succeed, quorum of {} on {}, sending commit to peers", record, quorum, writeQuorum);

                    apProtocolClient.commit(record.get_ID()).subscribe(
                            this::handleCommit,
                            this::handleError);
                } else {
                    log.info("Propose for {} failed, quorum of {} on {} needed", record, quorum, writeQuorum);

                    this.rollback();
                }

            }

            private void handleCommit(Long quorum) {
                if (quorum >= writeQuorum) {
                    log.info("Commit for {} succeed, quorum of {} on {}, sending commit to peers", record, quorum, writeQuorum);

                    this.saveRecord();
                } else {
                    log.info("Commit for {} failed, quorum of {} on {} needed", record, quorum, writeQuorum);

                    this.handleError(new Throwable("Commit for " + record.toString() + " failed, quorum of " + quorum + " on " + writeQuorum + " needed"));
                }
            }

            private void rollback() {
                log.info("Sending RollBack to peers for record {}", record);

                apProtocolClient.rollback(record.get_ID())
                        .doOnError(this::handleError).log("Error executing rollback")
                        .subscribe(this::handleRollBackResult);
            }

            private void saveRecord() {
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
