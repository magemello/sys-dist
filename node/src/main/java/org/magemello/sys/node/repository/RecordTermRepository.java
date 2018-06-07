package org.magemello.sys.node.repository;

import org.magemello.sys.node.domain.RecordTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.Optional;

@Repository
//@RepositoryRestResource(collectionResourceRel = "recordterm", path = "recordsterm")
public interface RecordTermRepository extends JpaRepository<RecordTerm, Long> {

    Optional<RecordTerm> findByKey(String key);

    Flux<RecordTerm> findByTermLessThanAndTickLessThan(Integer term, Integer tick);

}
