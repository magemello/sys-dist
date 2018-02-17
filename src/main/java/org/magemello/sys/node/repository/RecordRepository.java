package org.magemello.sys.node.repository;

import org.magemello.sys.node.domain.Record;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "record", path = "records")
public interface RecordRepository extends PagingAndSortingRepository<Record, Long> {

    Record findByKey(String key);
    
    void deleteAll();

}
