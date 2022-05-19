package com.boutiqaat.ingestor.repository;

import com.boutiqaat.ingestor.entity.CatalogProductSuperLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfigRepository extends JpaRepository<CatalogProductSuperLink, Long> {
}
