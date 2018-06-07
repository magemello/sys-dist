package org.magemello.sys.node.repository;

import java.util.Optional;

import org.magemello.sys.node.domain.Record;
import org.magemello.sys.node.domain.RecordTerm;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import reactor.core.publisher.Flux;

@RepositoryRestResource(collectionResourceRel = "record", path = "records")
public interface RecordRepository extends PagingAndSortingRepository<Record, Long> {

    void deleteAll();

    Optional<RecordTerm> findByKey(String key);

    Flux<RecordTerm> findByTermLessThanAndTickLessThan(Integer term, Integer tick);

}
