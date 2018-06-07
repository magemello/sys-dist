package org.magemello.sys.node.repository;

import org.magemello.sys.node.domain.RecordTerm;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import reactor.core.publisher.Flux;

@RepositoryRestResource(collectionResourceRel = "recordterm", path = "recordsterm")
public interface RecordTermRepository extends CrudRepository<RecordTerm, Long> {

    RecordTerm findByKey(String key);

    Flux<RecordTerm> findByTermLessThanAndTickLessThan(Integer term, Integer tick);

}
