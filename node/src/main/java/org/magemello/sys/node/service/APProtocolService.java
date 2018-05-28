package org.magemello.sys.node.service;

import org.magemello.sys.node.clients.APProtocolClient;
import org.magemello.sys.node.domain.Record;
import org.magemello.sys.node.domain.Transaction;
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
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Service("AP")
@RefreshScope
public class APProtocolService implements ProtocolService {

    private static final Logger log = LoggerFactory.getLogger(APProtocolService.class);

    @Autowired
    RecordRepository recordRepository;

    @Autowired
    private APProtocolClient apProtocolClient;

    private Map<String, Transaction> writeAheadLog = new LinkedHashMap<>();

    @Value("${read-quorum:2}")
    private Long readQuorum;

    @Value("${write-quorum:1}")
    private Long writeQuorum;

    @Override
    public Mono<ResponseEntity> get(String key) {
        log.info("AP Service - get for {} ", key);

        List<ClientResponse> clientResponses = apProtocolClient.read(key).collectList().block();

        Map.Entry<Record, Integer> entryRecord = getEntryRecordWithHighestQuorum(clientResponses, key);

        Record record = entryRecord.getKey();

        if (record != null) {
            sendRepair(clientResponses, record);

            if (entryRecord.getValue() >= readQuorum) {
                return Mono.just(ResponseEntity
                        .status(HttpStatus.OK)
                        .body("QUORUM " + record.toString()));
            } else {
                return Mono.just(ResponseEntity
                        .status(HttpStatus.CONFLICT).build());
            }
        } else {
            return Mono.just(ResponseEntity
                    .status(HttpStatus.NOT_FOUND).build());
        }
    }

    private void sendRepair(List<ClientResponse> clientResponses, Record record) {
        apProtocolClient.repair(clientResponses, record).subscribe(aBoolean -> {
            log.info("Repair outcome " + aBoolean);
        });

        if(!recordRepository.findByKey(record.getKey()).equals(record)){
            recordRepository.save(record);
        }
    }

    @Override
    public Mono<ResponseEntity> set(String key, String value) throws Exception {
        log.info("AP Service - Proposing to peers");
        Transaction transaction = new Transaction(key, value);

        return handleSet(transaction);
    }

    @Override
    public void reset(){
        writeAheadLog.clear();
    }

    @Override
    public String protocolName() {
        return "AP";
    }


    public boolean propose(Transaction transaction) {
        log.info("AP Service - Propose for {} ", transaction);

        if (isAProposalPresentFor(transaction.getKey())) {
            writeAheadLog.put(transaction.get_ID(), transaction);
            return true;
        }
        return false;
    }

    public Record commit(String id) {
        Transaction transaction = writeAheadLog.get(id);

        if (transaction != null) {
            Record record = recordRepository.save(new Record(transaction.getKey(), transaction.getValue()));
            writeAheadLog.remove(id);
            log.info("- succefullly committed proposal {}", id);
            return record;
        } else {
            log.info("- failed to find proposal {}", id);
            return null;
        }
    }

    public Transaction rollback(String id) {
        Transaction transaction = writeAheadLog.get(id);

        if (transaction != null) {
            transaction = writeAheadLog.remove(id);
            log.info("- succefullly rolled back proposal {}", id);
        } else {
            log.info("- failed to find proposal {}", id);
        }

        return transaction;
    }

    public Record repair(Record record) {
        log.info("- repair id {} ", record);

        return recordRepository.save(record);
    }

    public Record read(String key) {
        log.info("- read record for key {} ", key);
        return recordRepository.findByKey(key);
    }

    private Mono<ResponseEntity> handleSet(Transaction transaction) {
        return new Mono<ResponseEntity>() {

            CoreSubscriber<? super ResponseEntity> actual;

            @Override
            public void subscribe(CoreSubscriber<? super ResponseEntity> actual) {
                log.info("Sending proposal for {} to peers", transaction);

                this.actual = actual;

                apProtocolClient.propose(transaction)
                        .subscribe(this::handlePropose,
                                this::handleError);
            }

            private void handlePropose(List<ClientResponse> clientResponses) {
                Long quorum = clientResponses.stream().filter(clientResponse -> !clientResponse.statusCode().isError()).count();
                if (quorum >= writeQuorum) {
                    log.info("Propose for {} succeed, quorum of {} on {}, sending commit to peers", transaction, quorum, writeQuorum);

                    apProtocolClient.commit(transaction.get_ID())
                            .doOnError(this::handleError).log("Error executing commit")
                            .subscribe(this::handleCommit);
                } else {
                    log.info("Propose for {} failed, quorum of {} on {} needed", transaction, quorum, writeQuorum);

                    apProtocolClient.rollback(transaction.get_ID(), clientResponses)
                            .doOnError(this::handleError).log("Error executing rollback")
                            .subscribe(this::handleRollBackResult);
                }
            }

            private void handleCommit(Long quorum) {
                if (quorum >= writeQuorum) {
                    log.info("Commit for {} succeed, quorum of {} on {}, sending commit to peers", transaction, quorum, writeQuorum);

                    this.saveRecord();
                } else {
                    log.info("Commit for {} failed, quorum of {} on {} needed", transaction, quorum, writeQuorum);

                    this.handleError(new Throwable("Commit for " + transaction.toString() + " failed, quorum of " + quorum + " on " + writeQuorum + " needed"));
                }
            }

            private void saveRecord() {
                log.info("Peers Committed {}", transaction);

                recordRepository.save(new Record(transaction.getKey(), transaction.getValue()));

                actual.onNext(ResponseEntity
                        .status(HttpStatus.OK)
                        .body("Stored " + transaction.toString()));

                actual.onComplete();
            }

            private void handleRollBackResult(Boolean RollBack) {
                log.info("Peers Rolled Back {}", transaction);

                actual.onNext(ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body("Roll Backed " + transaction.toString()));
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

    private List<Record> getPeersRecords(List<ClientResponse> clientResponses, String key) {
        List<Record> records = clientResponsesToRecords(clientResponses);
        records.add(recordRepository.findByKey(key));
        return records;
    }

    private List<Record> clientResponsesToRecords(List<ClientResponse> clientResponses) {
        return clientResponses.stream().map(clientResponseFromStream -> clientResponseFromStream.bodyToMono(Record.class).block())
                .collect(Collectors.toList());
    }

    private Map.Entry<Record, Integer> getEntryRecordWithHighestQuorum(List<ClientResponse> clientResponses, String key) {
        List<Record> records = getPeersRecords(clientResponses, key);

        Map<Record, Integer> matches = new HashMap<>();

        for (Record record : records) {
            if (record != null) {
                Integer counter = matches.get(record);
                if (counter != null) {
                    counter++;
                    matches.put(record, counter);
                } else {
                    matches.put(record, 1);
                }

            }
        }

        Map<Record, Integer> result = matches.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));

        return result.entrySet().iterator().next();
    }

    private boolean isAProposalPresentFor(String key) {
        return this.writeAheadLog
                .entrySet()
                .stream()
                .allMatch(stringRecordEntry -> stringRecordEntry.getValue().getKey().equals(key));
    }
}
