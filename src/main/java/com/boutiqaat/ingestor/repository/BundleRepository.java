package com.boutiqaat.ingestor.repository;

import com.boutiqaat.ingestor.entity.BundleId;
import com.boutiqaat.ingestor.entity.CatalogProductBundleSelection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BundleRepository extends JpaRepository<CatalogProductBundleSelection, BundleId> {
    List<CatalogProductBundleSelection> findAllByIdParentProductIdIn(List<Integer> parentIds);
}
