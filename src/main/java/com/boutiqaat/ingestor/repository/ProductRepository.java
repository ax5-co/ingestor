package com.boutiqaat.ingestor.repository;

import com.boutiqaat.ingestor.entity.CatalogProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.util.List;

/**
 * Thanks to S for https://blog.microideation.com/2019/03/10/efficiently-batch-processing-records-from-large-table-in-spring-using-peeking/
 *
 */
@Repository
public interface ProductRepository extends JpaRepository<CatalogProductEntity, Long> {

    List<CatalogProductEntity> findAllByProductIdGreaterThanOrderByProductIdAsc(Long productId, Pageable pageable);
}
