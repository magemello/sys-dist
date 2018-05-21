package org.magemello.sys.node.service;

import org.magemello.sys.node.clients.ACProtocolClient;
import org.magemello.sys.node.domain.Record;
import org.magemello.sys.node.domain.Response;
import org.magemello.sys.node.repository.RecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
    public Record get(String key) {
        return recordRepository.findByKey(key);
    }

    @Override
    public Mono<Response> set(String key, String value) throws Exception {
        log.info("AC Service - Proposing to peers");
        Record record = new Record(key, value);

        return new Mono<Response>() {

            CoreSubscriber<? super Response> actual;

            @Override
            public void subscribe(CoreSubscriber<? super Response> actual) {
                this.actual = actual;

                acProtocolClient.vote(record)
                        .doOnError(this::handleVoteError).log("Error -> Sending rollback to peers")
                        .subscribe(this::handleVoteResult);
            }

            private void handleVoteResult(Boolean resultVote) {
                if (resultVote) {
                    log.info("Vote for {} succeed sending commit to peers", record);

                    acProtocolClient.commit(record.get_ID())
                            .doOnError(this::handleError).log("Error executing commit")
                            .subscribe(this::handleCommitResult);
                }
            }

            private void handleVoteError(Throwable error) {
                log.error("Vote for {} failed sending rollback to peers", record);

                acProtocolClient.rollback(record.get_ID())
                        .doOnError(this::handleError).log("Error executing rollback")
                        .subscribe(this::handleRollBackResult);
            }

            private void handleCommitResult(Boolean resultCommit) {
                log.info("Peers Committed {}", record);
                recordRepository.save(record);
                actual.onNext(new Response("Stored " + record.toString(), HttpStatus.OK));
                actual.onComplete();
            }

            private void handleRollBackResult(Boolean RollBack) {
                log.info("Peers Rolled Back {}", record);
                recordRepository.delete(record);
                actual.onNext(new Response("Roll Backed " + record.toString(), HttpStatus.BAD_REQUEST));
                actual.onComplete();
            }

            private void handleError(Throwable error) {
                actual.onNext(new Response(error.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR));
                actual.onComplete();
            }
        };
    }

    @Override
    public String protocolName() {
        return "AC";
    }

    public boolean vote(Record record) {
        log.info("AC Service - Vote for {} ", record);

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
            recordRepository.delete(record);
            writeAheadLog.remove(id);
            keys.remove(record.getKey());
        }
        return record;
    }

    public void clearWriteHaeadLog() {
        writeAheadLog.clear();
        keys.clear();
    }
}
