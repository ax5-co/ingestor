package com.boutiqaat.ingestor.repository;

import com.boutiqaat.ingestor.entity.CatalogProductEntityDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PriceRepository extends JpaRepository<CatalogProductEntityDecimal, Long> {
    List<CatalogProductEntityDecimal> findAllByProductIdIn(Iterable<Long> ids);
}
