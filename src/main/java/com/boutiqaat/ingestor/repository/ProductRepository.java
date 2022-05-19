package com.boutiqaat.ingestor.repository;

import com.boutiqaat.ingestor.entity.CatalogProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<CatalogProductEntity, Long> {
}
