package org.magemello.sys.node.protocols.ap.service;

import org.magemello.sys.node.protocols.ac.domain.Transaction;
import org.magemello.sys.node.protocols.ap.clients.APProtocolClient;
import org.magemello.sys.node.protocols.ap.domain.APRecord;
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
        log.info("\nAP Service - get for {} ", key);

        return new Mono<ResponseEntity>() {

            private CoreSubscriber<? super ResponseEntity> actual;

            AtomicBoolean returnedValue = new AtomicBoolean(false);

            List<String> matches = Collections.synchronizedList(new ArrayList<String>());

            @Override
            public void subscribe(CoreSubscriber<? super ResponseEntity> actual) {
                this.actual = actual;
                APRecord record = (APRecord) recordRepository.findByKey(key).orElse(null);
                if (record != null) {
                    matches.add(record.getVal());
                } else {
                    matches.add(null);
                }

                if (readQuorum == 1) {
                    returnedValue.set(true);
                    returnValue(record);
                }

                apProtocolClient.read(key).map(this::manageReadQuorum).collectList().subscribe((List<ResponseEntity<APRecord>> responseEntities) -> {
                    log.info("\n - Sending repair to discording peers");

                    matches.stream().collect(Collectors.groupingBy(s -> s, Collectors.counting()))
                            .entrySet()
                            .stream()
                            .max(Comparator.comparing(Map.Entry::getValue))
                            .ifPresent(stringLongEntry -> {
                                sendRepair(responseEntities, new APRecord(key, stringLongEntry.getKey()));
                            });
                });
            }

            private ResponseEntity<APRecord> manageReadQuorum(ResponseEntity<APRecord> apRecordResponseEntity) {
                log.info("\n - Val {} from {}", apRecordResponseEntity.getBody(), apRecordResponseEntity.getHeaders().get("x-sys-ip"));

                APRecord record = apRecordResponseEntity.getBody();
                Long matchRecordCounter;

                if (record != null && record.getVal() != null) {
                    matches.add(record.getVal());
                    matchRecordCounter = matches.stream().filter(s -> s != null && s.equals(record.getVal())).count();
                } else {
                    matches.add(null);
                    matchRecordCounter = matches.stream().filter(s -> s == null).count();
                }

                if (matchRecordCounter >= readQuorum) {
                    if (!returnedValue.getAndSet(true)) {
                        returnValue(record);
                    }
                }

                return apRecordResponseEntity;
            }

            private void returnValue(APRecord record) {
                if (record != null) {
                    actual.onNext(ResponseEntity
                            .status(HttpStatus.OK)
                            .body(record));
                    actual.onComplete();
                } else {
                    actual.onNext(ResponseEntity
                            .status(HttpStatus.NOT_FOUND).build());
                    actual.onComplete();
                }
            }
        };
    }

    private void sendRepair(List<ResponseEntity<APRecord>> responseEntity, APRecord record) {
        apProtocolClient.repair(responseEntity, record).subscribe(clientResponse -> {
            log.info("\nAP Service - Repair {} status {}",
                    clientResponse.headers().header("x-sys-ip").stream().findFirst().get(),
                    clientResponse.statusCode());
        });

        APRecord localRecord = (APRecord) recordRepository.findByKey(record.getKey()).orElse(null);
        if (localRecord == null || !localRecord.getVal().equals(record.getVal())) {
            recordRepository.save(record);
        }
    }

    @Override
    public Mono<ResponseEntity> set(String key, String value) throws Exception {
        log.info("\nAP Service - Proposing to peers");
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
            log.info("\n- accepted proposal {} for key {}", transaction.get_ID(), transaction.getKey());
            writeAheadLog.put(transaction.get_ID(), transaction);
            return true;
        } else {
            log.info("\n- refused proposal {} for key {} (already present)", transaction.get_ID(), transaction.getKey());
            return false;
        }
    }

    public APRecord commit(String id) {
        Transaction transaction = writeAheadLog.get(id);

        if (transaction != null) {
            APRecord record = recordRepository.save(new APRecord(transaction.getKey(), transaction.getValue()));
            writeAheadLog.remove(id);
            log.info("\n- successfully committed proposal {}", id);
            return record;
        } else {
            log.info("\n- failed to find proposal {}", id);
            return null;
        }
    }

    public Transaction rollback(String id) {
        Transaction transaction = writeAheadLog.get(id);

        if (transaction != null) {
            transaction = writeAheadLog.remove(id);
            log.info("\n- successfully rolled back proposal {}", id);
        } else {
            log.info("\n- failed to find proposal {}", id);
        }

        return transaction;
    }

    public APRecord repair(APRecord record) {
        log.info("\n- repair id {} ", record);

        return recordRepository.save(record);
    }

    public APRecord read(String key) {
        log.info("\n- read record for key {} ", key);
        return (APRecord) recordRepository.findByKey(key).orElse(null);
    }

    private Mono<ResponseEntity> handleSet(Transaction transaction) {
        return new Mono<ResponseEntity>() {

            private CoreSubscriber<? super ResponseEntity> actual;

            AtomicInteger commitQuorum = new AtomicInteger(0);
            AtomicBoolean returnedValue = new AtomicBoolean(false);

            @Override
            public void subscribe(CoreSubscriber<? super ResponseEntity> actual) {
                log.info("\nSending proposal for {} to peers", transaction);

                this.actual = actual;

                apProtocolClient.propose(transaction)
                        .subscribe(this::handlePropose,
                                this::handleError);
            }

            private void handlePropose(List<ClientResponse> clientResponses) {
                Long quorum = clientResponses.stream().filter(clientResponse -> !clientResponse.statusCode().isError()).count();
                if (quorum >= writeQuorum) {
                    log.info("\nPropose for {} succeed, quorum of {} on {}, sending commit to peers", transaction, quorum, writeQuorum);

                    apProtocolClient.commit(transaction.get_ID())
                            .map(this::manageCommitQuorum).collectList()
                            .subscribe(this::handleCommit,
                                    this::handleError);
                } else {
                    log.info("\nPropose for {} failed, quorum of {} on {} needed", transaction, quorum, writeQuorum);

                    apProtocolClient.rollback(transaction.get_ID(), clientResponses)
                            .subscribe(this::handleRollBackResult,
                                    this::handleError);
                }
            }

            private ClientResponse manageCommitQuorum(ClientResponse clientResponse) {
                if (!clientResponse.statusCode().isError()) {
                    if (commitQuorum.incrementAndGet() > writeQuorum) {
                        if (!returnedValue.getAndSet(true)) {
                            APRecord record = new APRecord(transaction.getKey(), transaction.getValue());
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
                    log.info("\nCommit for {} succeed, quorum of {} on {} needed", transaction, quorum, writeQuorum);
                } else {
                    log.info("\nCommit for {} failed, quorum of {} on {} needed", transaction, quorum, writeQuorum);

                    this.handleError(new Throwable("Commit for " + transaction.toString() + " failed, quorum of " + quorum + " on " + writeQuorum + " needed"));
                }
            }

            private void handleRollBackResult(Boolean RollBack) {
                log.info("\nPeers Rolled Back {}", transaction);

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

    private boolean isAProposalPresentFor(String key) {
        return this.writeAheadLog
                .entrySet()
                .stream()
                .anyMatch(stringRecordEntry -> stringRecordEntry.getValue().getKey().equals(key));
    }

    @Override
    public void start() {
        log.info("\nAP mode (sloppy quorums)");

    }

    @Override
    public void stop() {
    }
}
