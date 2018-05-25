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
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

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

        return handleGet(key);
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

    public boolean checkValue(Record record) {
        log.info("AP Service - check value for {} ", record);
        return recordRepository.findByKey(record.getKey()).equals(record);
    }

    public void clearWriteHeadLog() {
        writeAheadLog.clear();
        keys.clear();
    }

    private Mono<ResponseEntity> handleGet(String key) {
        return new Mono<ResponseEntity>() {

            CoreSubscriber<? super ResponseEntity> actual;

            @Override
            public void subscribe(CoreSubscriber<? super ResponseEntity> actual) {
                this.actual = actual;

                Flux<ClientResponse> responseFlux = apProtocolClient.read(key);

                responseFlux.filter(clientResponse -> !clientResponse.statusCode().isError())
                        .flatMap(clientResponse -> clientResponse.bodyToMono(Record.class))
                        .groupBy(record -> record)
                        .doOnNext(recordRecordGroupedFlux -> {
                            recordRecordGroupedFlux.count().filter(count -> count >= readQuorum).doOnNext(aLong -> {

                                responseFlux.filter(clientResponse -> clientResponse.statusCode().isError())
                                        .doOnNext(clientResponse -> {
                                            apProtocolClient.repair(recordRecordGroupedFlux.key());
                                        });

                                responseFlux.flatMap(clientResponse -> clientResponse.bodyToMono(Record.class))
                                        .filter(record -> !record.getValue().equals(recordRecordGroupedFlux.key().getValue()))
                                        .doOnNext(clientResponse -> {
                                            apProtocolClient.repair(recordRecordGroupedFlux.key());
                                        });

                                recordRepository.save(recordRecordGroupedFlux.key());
                                actual.onNext(ResponseEntity
                                        .status(HttpStatus.OK)
                                        .body(recordRecordGroupedFlux.key()));
                                actual.onComplete();                            });
                        });
            }
        };
    }

    private Mono<ResponseEntity> handleSet(Record record) {
        return new Mono<ResponseEntity>() {

            CoreSubscriber<? super ResponseEntity> actual;

            @Override
            public void subscribe(CoreSubscriber<? super ResponseEntity> actual) {
                this.actual = actual;

                apProtocolClient.propose(record)
                        .filter(quorumVote -> quorumVote >= writeQuorum)
                        .log("Propose for " + record.toString() + " succeed sending commit to peers")
                        .doOnNext(this::commit)
                        .filter(quorumVote -> quorumVote < writeQuorum)
                        .doOnNext(this::rollback);
            }

            private void commit(Long quorum) {
                log.info("Propose for {} succeed, quorum of {} on {}, sending commit to peers", record, quorum, writeQuorum);

                apProtocolClient.commit(record.get_ID())
                        .filter(quorumVote -> quorumVote >= writeQuorum)
                        .log("Propose for " + record.toString() + " succeed sending commit to peers")
                        .doOnNext(this::saveRecord)
                        .filter(quorumVote -> quorumVote < writeQuorum)
                        .doOnNext(this::commitError);
            }

            private void rollback(Long quorum) {
                log.info("Propose for {} failed, quorum of {} on {} needed", record, quorum, writeQuorum);
                log.info("Sending RollBack to peers for record {}", record);

                apProtocolClient.rollback(record.get_ID())
                        .doOnError(this::handleError).log("Error executing rollback")
                        .subscribe(this::handleRollBackResult);
            }

            private void saveRecord(Long quorumVote) {
                log.info("Peers Committed {}", record);

                recordRepository.save(record);

                actual.onNext(ResponseEntity
                        .status(HttpStatus.OK)
                        .body("Stored " + record.toString()));

                actual.onComplete();
            }

            private void commitError(Long quorumVote) {
                log.error("Commit for {} failed, quorum of {} on {} needed", record, quorumVote, writeQuorum);

                actual.onNext(ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Commit for " + record.toString() + " failed, quorum of " + quorumVote.toString() + " on " +
                                "" + writeQuorum.toString() + " " + "needed"));
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
