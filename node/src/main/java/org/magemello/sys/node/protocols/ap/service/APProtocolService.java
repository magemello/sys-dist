package org.magemello.sys.node.protocols.ap.service;

import org.magemello.sys.node.domain.Record;
import org.magemello.sys.node.domain.Transaction;
import org.magemello.sys.node.protocols.ap.clients.APProtocolClient;
import org.magemello.sys.node.repository.RecordRepository;
import org.magemello.sys.node.service.ProtocolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service("AP")
@SuppressWarnings("rawtypes")
public class APProtocolService implements ProtocolService {

    private static final Logger log = LoggerFactory.getLogger(APProtocolService.class);

    @Autowired
    RecordRepository recordRepository;

    @Autowired
    private APProtocolClient apProtocolClient;

    private Map<String, Transaction> writeAheadLog = new LinkedHashMap<>();

    @Value("${read-quorum:2}")
    private Integer readQuorum;

    @Value("${write-quorum:1}")
    private Integer writeQuorum;

    @Override
    public Mono<ResponseEntity> get(String key) {
        log.info("AP Service - get for {} ", key);

        List<ResponseEntity<Record>> responseEntity = apProtocolClient.read(key).collectList().block();

        Map.Entry<Record, Integer> entryRecord = getEntryRecordWithHighestQuorum(responseEntity, key);

        if (entryRecord != null && entryRecord.getKey() != null) {
            Record record = entryRecord.getKey();

            sendRepair(responseEntity, record);

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

    private void sendRepair(List<ResponseEntity<Record>> responseEntity, Record record) {
        apProtocolClient.repair(responseEntity, record).subscribe(clientResponse -> {
            log.info("AP Service - Repair {} status {}",
                    clientResponse.headers().header("x-sys-ip").stream().findFirst().get(),
                    clientResponse.statusCode());
        });

        Record localRecord = recordRepository.findByKey(record.getKey()).orElse(null);
        if (localRecord == null || !localRecord.equals(record)) {
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
    public void onCleanup() {
        writeAheadLog.clear();
    }

    @Override
    public String protocolName() {
        return "AP";
    }


    public boolean propose(Transaction transaction) {
        if (!isAProposalPresentFor(transaction.getKey())) {
            log.info("- accepted proposal {} for key {}", transaction.get_ID(), transaction.getKey());
            writeAheadLog.put(transaction.get_ID(), transaction);
            return true;
        } else {
            log.info("- refused proposal {} for key {} (already present)", transaction.get_ID(), transaction.getKey());
            return false;
        }
    }

    public Record commit(String id) {
        Transaction transaction = writeAheadLog.get(id);

        if (transaction != null) {
            Record record = recordRepository.save(new Record(transaction.getKey(), transaction.getValue()));
            writeAheadLog.remove(id);
            log.info("- successfully committed proposal {}", id);
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
            log.info("- successfully rolled back proposal {}", id);
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
        return recordRepository.findByKey(key).orElse(null);
    }

    private Mono<ResponseEntity> handleSet(Transaction transaction) {
        return new Mono<ResponseEntity>() {

            private CoreSubscriber<? super ResponseEntity> actual;

            AtomicInteger commitQuorum = new AtomicInteger(0);
            AtomicBoolean returnedValue = new AtomicBoolean(false);

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
                            .map(this::manageCommitQuorum).collectList()
                            .subscribe(this::handleCommit,
                                    this::handleError);
                } else {
                    log.info("Propose for {} failed, quorum of {} on {} needed", transaction, quorum, writeQuorum);

                    apProtocolClient.rollback(transaction.get_ID(), clientResponses)
                            .subscribe(this::handleRollBackResult,
                                    this::handleError);
                }
            }

            private ClientResponse manageCommitQuorum(ClientResponse clientResponse) {
                if (!clientResponse.statusCode().isError()) {
                    if (commitQuorum.incrementAndGet() > writeQuorum) {
                        if (!returnedValue.getAndSet(true)) {
                            Record record = new Record(transaction.getKey(), transaction.getValue());
                            recordRepository.save(record);

                            actual.onNext(ResponseEntity
                                    .status(HttpStatus.OK)
                                    .body("Stored " + record.toString()));
                            actual.onComplete();
                        }
                    }
                }

                return clientResponse;
            }

            private void handleCommit(List<ClientResponse> clientResponses) {
                Integer quorum = commitQuorum.get();

                if (quorum >= writeQuorum) {
                    log.info("Commit for {} succeed, quorum of {} on {} needed", transaction, quorum, writeQuorum);
                } else {
                    log.info("Commit for {} failed, quorum of {} on {} needed", transaction, quorum, writeQuorum);

                    this.handleError(new Throwable("Commit for " + transaction.toString() + " failed, quorum of " + quorum + " on " + writeQuorum + " needed"));
                }
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
                        .status(HttpStatus.REQUEST_TIMEOUT)
                        .body(error.getMessage()));
                actual.onComplete();
            }
        };
    }

    private List<Record> getPeersRecords(List<ResponseEntity<Record>> responseEntity, String key) {
        List<Record> records = responseEntity.stream()
            .map(responseEntityFromStream -> responseEntityFromStream.getBody())
            .collect(Collectors.toList());

        records.add(recordRepository.findByKey(key).orElse(null));
        return records;
    }

    private Map.Entry<Record, Integer> getEntryRecordWithHighestQuorum(List<ResponseEntity<Record>> responseEntity, String key) {
        List<Record> records = getPeersRecords(responseEntity, key);

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

        if (result.entrySet().size() > 0) {
            return result.entrySet().iterator().next();
        }

        return null;
    }

    private boolean isAProposalPresentFor(String key) {
        return this.writeAheadLog
                .entrySet()
                .stream()
                .anyMatch(stringRecordEntry -> stringRecordEntry.getValue().getKey().equals(key));
    }

    @Override
    public void start() {
        log.info("AP mode (sloppy quorums)");

    }

    @Override
    public void stop() {
    }
}
