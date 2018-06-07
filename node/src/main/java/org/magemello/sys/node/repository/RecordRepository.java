package org.magemello.sys.node.repository;

import java.util.ArrayList;
import java.util.Optional;

import org.magemello.sys.node.domain.Record;
import org.magemello.sys.node.domain.RecordTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "record", path = "records")
public interface RecordRepository extends JpaRepository<Record, Long> {
    void deleteAll();

    Optional<RecordTerm> findByKey(String key);

    ArrayList<RecordTerm> findByTermLessThanEqualAndTickLessThanEqual(Integer term, Integer tick);
}
